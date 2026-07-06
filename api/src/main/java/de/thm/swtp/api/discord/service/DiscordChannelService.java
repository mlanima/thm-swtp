package de.thm.swtp.api.discord.service;

import de.thm.swtp.api.discord.client.BotInternalClient;
import de.thm.swtp.api.discord.entity.DiscordChannelSettingsEntity;
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

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscordChannelService {

    private final LinkedChannelRepository linkedChannelRepository;
    private final DiscordChannelSettingsRepository settingsRepository;
    private final ProjectRepository projectRepository;
    private final BotInternalClient botInternalClient;

    @Transactional
    public LinkedChannelEntity connectChannel(UUID projectId, String discordChannelId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        linkedChannelRepository.findByProjectId(projectId)
                .filter(LinkedChannelEntity::isActive)
                .ifPresent(link -> {
                    throw new DiscordConnectionFailedException("Project already has an active Discord channel link");
                });

        BotInternalClient.TestConnectionResponse test = botInternalClient.testConnection(discordChannelId);
        if (!test.success()) {
            throw new DiscordConnectionFailedException(
                    "Bot cannot access channel: " + (test.reason() != null ? test.reason() : "unknown reason"));
        }

        LinkedChannelEntity link = linkedChannelRepository.findByProjectId(projectId)
                .map(existing -> {
                    existing.setDiscordChannelId(discordChannelId);
                    existing.setActive(true);
                    return existing;
                })
                .orElseGet(() -> LinkedChannelEntity.builder()
                        .project(project)
                        .discordChannelId(discordChannelId)
                        .isActive(true)
                        .build());

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

        link.setActive(false);
        linkedChannelRepository.save(link);
        log.info("Discord channel disconnected: project={}, channelId={}", projectId, link.getDiscordChannelId());
    }

    @Transactional(readOnly = true)
    public LinkedChannelEntity getLinkedChannel(UUID projectId) {
        return linkedChannelRepository.findByProjectId(projectId)
                .orElseThrow(() -> new DiscordConnectionFailedException("No Discord channel linked to this project"));
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
