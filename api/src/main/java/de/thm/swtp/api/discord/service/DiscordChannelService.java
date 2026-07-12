package de.thm.swtp.api.discord.service;

import de.thm.swtp.api.discord.client.BotInternalClient.GuildInfo;
import de.thm.swtp.api.discord.client.BotInternalClient;
import de.thm.swtp.api.discord.config.DiscordProperties;
import de.thm.swtp.api.discord.entity.DiscordChannelSettingsEntity;
import de.thm.swtp.api.discord.dto.DiscordChannelResponse;
import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import de.thm.swtp.api.discord.exception.DiscordConnectionFailedException;
import de.thm.swtp.api.discord.repository.DiscordChannelSettingsRepository;
import de.thm.swtp.api.discord.repository.LinkedChannelRepository;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscordChannelService {

    private final LinkedChannelRepository linkedChannelRepository;
    private final DiscordChannelSettingsRepository settingsRepository;
    private final ProjectRepository projectRepository;
    private final BotInternalClient botInternalClient;
    private final DiscordAuthService discordAuthService;
    private final DiscordProperties discordProperties;

    @Transactional
    public LinkedChannelEntity connectChannel(UUID projectId, String discordChannelId) {
        return connectChannel(projectId, discordChannelId, null);
    }

    @Transactional
    public LinkedChannelEntity connectChannel(UUID projectId, String discordChannelId, String discordGuildId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (project.getOwner().getDiscordId() == null || project.getOwner().getDiscordId().isBlank()) {
            throw new DiscordConnectionFailedException(
                    "You must link your Discord account in profile settings before connecting a Discord server");
        }

        linkedChannelRepository.findByProjectId(projectId)
                .filter(LinkedChannelEntity::isActive)
                .ifPresent(link -> {
                    link.setActive(false);
                    linkedChannelRepository.save(link);
                    log.info("Discord channel deactivated for reconnect: project={}, oldChannelId={}",
                            projectId, link.getDiscordChannelId());
                });

        if (discordGuildId != null) {
            linkedChannelRepository.findAllByDiscordGuildIdAndIsActiveTrue(discordGuildId)
                    .stream()
                    .filter(link -> !link.getProject().getId().equals(projectId))
                    .forEach(link -> {
                        link.setActive(false);
                        linkedChannelRepository.save(link);
                        log.info("Guild re-bound: deactivated old link for project={}, new project={}",
                                link.getProject().getId(), projectId);
                    });
        }

        linkedChannelRepository.findByDiscordChannelIdAndIsActiveTrue(discordChannelId)
                .ifPresent(link -> {
                    if (!link.getProject().getId().equals(projectId)) {
                        throw new DiscordConnectionFailedException(
                                "This Discord channel is already linked to another project");
                    }
                });

        BotInternalClient.TestConnectionResponse test = botInternalClient.testConnection(discordChannelId, discordGuildId);
        if (!test.success()) {
            throw new DiscordConnectionFailedException(
                    "Bot cannot access channel: " + (test.reason() != null ? test.reason() : "unknown reason"));
        }

        LinkedChannelEntity link = linkedChannelRepository.findByProjectId(projectId)
                .filter(l -> !l.isActive())
                .map(existing -> {
                    existing.setDiscordChannelId(discordChannelId);
                    existing.setDiscordGuildId(discordGuildId);
                    existing.setActive(true);
                    existing.setDiscordInviteUrl(null);
                    return existing;
                })
                .orElseGet(() -> LinkedChannelEntity.builder()
                        .project(project)
                        .discordChannelId(discordChannelId)
                        .discordGuildId(discordGuildId)
                        .isActive(true)
                        .build());

        BotInternalClient.CreateInviteResponse inviteResp = botInternalClient.createChannelInvite(discordChannelId);
        if (inviteResp.success() && inviteResp.inviteUrl() != null) {
            link.setDiscordInviteUrl(inviteResp.inviteUrl());
        }

        LinkedChannelEntity saved = linkedChannelRepository.save(link);

        if (settingsRepository.findByLinkedChannelId(saved.getId()).isEmpty()) {
            DiscordChannelSettingsEntity defaultSettings = DiscordChannelSettingsEntity.builder()
                    .linkedChannel(saved)
                    .build();
            settingsRepository.save(defaultSettings);
        }

        log.info("Discord channel linked: project={}, channelId={}", projectId, discordChannelId);
        return saved;
    }

    @Transactional
    public void disconnectChannel(UUID projectId) {
        LinkedChannelEntity link = linkedChannelRepository.findByProjectId(projectId)
                .orElseThrow(() -> new DiscordConnectionFailedException("No Discord channel linked to this project"));

        BotInternalClient.LeaveGuildResponse leaveResp = botInternalClient.leaveGuild(link.getDiscordChannelId());
        if (!leaveResp.success()) {
            log.warn("Bot failed to leave guild for project={}, channelId={}: {}",
                    projectId, link.getDiscordChannelId(), leaveResp.reason());
        }

        link.setActive(false);
        link.setDiscordInviteUrl(null);
        linkedChannelRepository.save(link);
        log.info("Discord channel disconnected: project={}, channelId={}", projectId, link.getDiscordChannelId());
    }

    @Transactional(readOnly = true)
    public LinkedChannelEntity getLinkedChannel(UUID projectId) {
        return linkedChannelRepository.findByProjectId(projectId)
                .orElseThrow(() -> new DiscordConnectionFailedException("No Discord channel linked to this project"));
    }

    public String getBotInviteUrl(UUID projectId, UUID userId) {
        return discordAuthService.createBotAuthUrl(
                projectId, userId,
                String.valueOf(discordProperties.getBot().getInvitePermissions()),
                discordAuthService.getRedirectUri());
    }

    @Transactional
    public LinkedChannelEntity updateDiscordInviteUrl(UUID projectId, String inviteUrl) {
        LinkedChannelEntity link = linkedChannelRepository.findByProjectId(projectId)
                .orElseThrow(() -> new DiscordConnectionFailedException("No Discord channel linked to this project"));

        link.setDiscordInviteUrl(inviteUrl);
        LinkedChannelEntity saved = linkedChannelRepository.save(link);
        log.info("Discord invite URL updated: project={}, url={}", projectId, inviteUrl);
        return saved;
    }

    public List<GuildInfo> getAvailableGuilds(UUID projectId) {
        var resp = botInternalClient.getGuilds();
        if (!resp.success() || resp.guilds() == null) {
            throw new DiscordConnectionFailedException(
                    "Failed to fetch guilds: " + (resp.reason() != null ? resp.reason() : "unknown error"));
        }

        var linkedGuildIds = linkedChannelRepository.findAll().stream()
                .filter(LinkedChannelEntity::isActive)
                .map(LinkedChannelEntity::getDiscordGuildId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return resp.guilds().stream()
                .filter(g -> !linkedGuildIds.contains(g.id()))
                .toList();
    }

    @Transactional
    public DiscordChannelResponse autoConnectChannel(UUID projectId) {
        return autoConnectChannel(projectId, null);
    }

    @Transactional
    public DiscordChannelResponse autoConnectChannel(UUID projectId, String guildId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (project.getOwner().getDiscordId() == null || project.getOwner().getDiscordId().isBlank()) {
            throw new DiscordConnectionFailedException(
                    "You must link your Discord account in profile settings before connecting a Discord server");
        }

        var effectiveGuildId = guildId != null ? guildId : discordAuthService.consumeBotGuild(projectId);

        BotInternalClient.AutoSetupResponse autoResp = botInternalClient.autoSetup(
                project.getOwner().getDiscordId(), effectiveGuildId);
        if (!autoResp.success() || autoResp.channelId() == null) {
            throw new DiscordConnectionFailedException(
                    "Bot could not auto-setup: " + (autoResp.reason() != null ? autoResp.reason() : "unknown error"));
        }

        BotInternalClient.TestConnectionResponse test = botInternalClient.testConnection(
                autoResp.channelId(), autoResp.guildId());
        if (!test.success()) {
            throw new DiscordConnectionFailedException(
                    "Bot cannot access auto-created channel: " + (test.reason() != null ? test.reason() : "unknown reason"));
        }

        LinkedChannelEntity link = connectChannel(projectId, autoResp.channelId(), autoResp.guildId());

        String warning = !autoResp.canWrite() ? "PROJECTSETTINGS.DISCORD.BOT_NO_WRITE_PERMISSION" : null;
        return DiscordChannelResponse.from(link, warning);
    }

    @Transactional(readOnly = true)
    public DiscordChannelSettingsEntity getSettings(UUID projectId) {
        LinkedChannelEntity link = getLinkedChannel(projectId);
        return settingsRepository.findByLinkedChannelId(link.getId())
                .orElseThrow(() -> new RuntimeException("Settings not found for linked channel"));
    }

    @Transactional
    public DiscordChannelSettingsEntity updateSettings(UUID projectId, DiscordChannelSettingsEntity updated) {
        LinkedChannelEntity link = getLinkedChannel(projectId);
        DiscordChannelSettingsEntity settings = settingsRepository.findByLinkedChannelId(link.getId())
                .orElseThrow(() -> new RuntimeException("Settings not found for linked channel"));

        settings.setNotifyPostCreated(updated.isNotifyPostCreated());
        settings.setNotifyPostUpdated(updated.isNotifyPostUpdated());
        settings.setNotifyPostDeleted(updated.isNotifyPostDeleted());
        settings.setNotifyMemberJoin(updated.isNotifyMemberJoin());
        settings.setNotifyMemberLeave(updated.isNotifyMemberLeave());

        DiscordChannelSettingsEntity saved = settingsRepository.save(settings);
        log.info("Discord channel settings updated: project={}", projectId);
        return saved;
    }
}
