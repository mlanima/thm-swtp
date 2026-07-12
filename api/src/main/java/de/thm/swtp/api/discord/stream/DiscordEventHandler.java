package de.thm.swtp.api.discord.stream;

import de.thm.swtp.api.discord.entity.DiscordMessageSyncEntity;
import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import de.thm.swtp.api.discord.repository.DiscordMessageSyncRepository;
import de.thm.swtp.api.discord.repository.LinkedChannelRepository;
import de.thm.swtp.api.projectPost.domain.ProjectPostStatus;
import de.thm.swtp.api.projectPost.entity.ProjectPostEntity;
import de.thm.swtp.api.projectPost.repository.ProjectPostRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiscordEventHandler {

    private final DiscordMessageSyncRepository messageSyncRepository;
    private final LinkedChannelRepository linkedChannelRepository;
    private final ProjectPostRepository projectPostRepository;
    private final UserProfileRepository userProfileRepository;
    private final DiscordEventPublisher discordEventPublisher;

    @Transactional
    public void handleDiscordMessageCreated(Map<String, String> payload) {
        String discordMsgId = payload.get("discordMsgId");
        String channelId = payload.get("channelId");
        String content = payload.get("content");
        String discordUserId = payload.get("discordUserId");
        String discordUsername = payload.get("discordUsername");

        if (messageSyncRepository.existsByDiscordMessageId(discordMsgId)) {
            log.debug("Duplicate discord message {}, skipping", discordMsgId);
            return;
        }

        Optional<LinkedChannelEntity> linkOpt = linkedChannelRepository.findByDiscordChannelIdAndIsActiveTrue(channelId);
        if (linkOpt.isEmpty()) {
            log.debug("No active linked channel for {}", channelId);
            return;
        }

        LinkedChannelEntity link = linkOpt.get();
        var project = link.getProject();

        UserProfile author = userProfileRepository.findByDiscordId(discordUserId)
                .orElseGet(() -> {
                    UserProfile ghost = UserProfile.builder()
                            .keycloakId(UUID.randomUUID())
                            .username(discordUsername + "#discord")
                            .discordId(discordUserId)
                            .discordUsername(discordUsername)
                            .build();
                    return userProfileRepository.save(ghost);
                });

        ProjectPostEntity post = ProjectPostEntity.builder()
                .project(project)
                .author(author)
                .title("Discord message")
                .content(content != null ? content : "")
                .status(ProjectPostStatus.PUBLISHED)
                .publishedAt(LocalDateTime.now())
                .build();

        try {
            ProjectPostEntity saved = projectPostRepository.save(post);
            DiscordMessageSyncEntity sync = DiscordMessageSyncEntity.builder()
                    .platformPostId(saved.getId())
                    .discordMessageId(discordMsgId)
                    .discordChannelId(channelId)
                    .direction(DiscordMessageSyncEntity.SyncDirection.DISCORD_TO_PLATFORM)
                    .build();
            messageSyncRepository.save(sync);
            log.info("Discord message synced to platform post: discordMsgId={}, postId={}", discordMsgId, saved.getId());
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate discord message {} (race condition)", discordMsgId);
        }
    }

    @Transactional
    public void handleDiscordMessageUpdated(Map<String, String> payload) {
        String discordMsgId = payload.get("discordMsgId");
        String content = payload.get("content");

        messageSyncRepository.findByDiscordMessageId(discordMsgId).ifPresent(sync -> {
            projectPostRepository.findById(sync.getPlatformPostId()).ifPresent(post -> {
                post.setContent(content != null ? content : "");
                projectPostRepository.save(post);
                log.info("Post updated from discord: postId={}", post.getId());
            });
        });
    }

    @Transactional
    public void handleDiscordMessageDeleted(Map<String, String> payload) {
        String discordMsgId = payload.get("discordMsgId");

        messageSyncRepository.findByDiscordMessageId(discordMsgId).ifPresent(sync -> {
            projectPostRepository.findById(sync.getPlatformPostId()).ifPresent(post -> {
                projectPostRepository.delete(post);
                log.info("Post deleted from discord sync: postId={}", post.getId());
            });
            messageSyncRepository.delete(sync);
        });
    }

    @Transactional
    public void handleMessageAssigned(Map<String, String> payload) {
        String postId = payload.get("postId");
        String discordMsgId = payload.get("discordMsgId");
        String channelId = payload.get("channelId");
        String guildId = payload.get("guildId");

        if (messageSyncRepository.existsByDiscordMessageId(discordMsgId)) {
            return;
        }

        UUID postUuid = UUID.fromString(postId);
        boolean postExistsAndPublished = projectPostRepository.findById(postUuid)
                .map(post -> post.getStatus() == ProjectPostStatus.PUBLISHED)
                .orElse(false);

        if (!postExistsAndPublished) {
            log.warn("Post {} no longer published — deleting orphaned Discord message {}",
                    postId, discordMsgId);
            discordEventPublisher.publishDirectDelete(postUuid, discordMsgId, channelId);
            return;
        }

        DiscordMessageSyncEntity sync = DiscordMessageSyncEntity.builder()
                .platformPostId(postUuid)
                .discordMessageId(discordMsgId)
                .discordChannelId(channelId)
                .discordGuildId(guildId)
                .direction(DiscordMessageSyncEntity.SyncDirection.PLATFORM_TO_DISCORD)
                .build();
        messageSyncRepository.save(sync);
        log.info("Message assigned: postId={}, discordMsgId={}", postId, discordMsgId);
    }

    @Transactional
    public void handleInviteResponse(Map<String, String> payload) {
        String inviteId = payload.get("inviteId");
        String response = payload.get("response");

        log.info("Invite response received: inviteId={}, response={}", inviteId, response);
    }

    @Transactional
    public void handleChannelDisconnected(Map<String, String> payload) {
        String channelId = payload.get("channelId");
        String reason = payload.get("reason");

        linkedChannelRepository.findByDiscordChannelId(channelId).ifPresent(link -> {
            link.setActive(false);
            linkedChannelRepository.save(link);
            log.warn("Channel disconnected: channelId={}, reason={}", channelId, reason);
        });
    }
}
