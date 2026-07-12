package de.thm.swtp.api.discord.stream;

import de.thm.swtp.api.discord.entity.DiscordMessageSyncEntity;
import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import de.thm.swtp.api.discord.repository.DiscordMessageSyncRepository;
import de.thm.swtp.api.discord.repository.LinkedChannelRepository;
import de.thm.swtp.api.discord.stream.payload.*;
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
import java.util.UUID;

/**
 * Handles all inbound events from the Discord bot that arrive via the Redis stream.
 * Each method maps to one EventType and performs the corresponding action on the platform side.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DiscordEventHandler {

    private final DiscordMessageSyncRepository messageSyncRepository;
    private final LinkedChannelRepository linkedChannelRepository;
    private final ProjectPostRepository projectPostRepository;
    private final UserProfileRepository userProfileRepository;
    private final DiscordEventPublisher discordEventPublisher;

    /**
     * Imports a Discord message as a new platform post.
     * Creates a ghost user profile if the Discord user isn't known yet.
     */
    @Transactional
    public void handleDiscordMessageCreated(DiscordMessageCreatedPayload payload) {
        String discordMsgId = payload.discordMsgId();
        String channelId = payload.channelId();
        String content = payload.content();
        String discordUserId = payload.discordUserId();
        String discordUsername = payload.discordUsername();

        if (messageSyncRepository.existsByDiscordMessageId(discordMsgId)) {
            log.debug("Duplicate discord message {}, skipping", discordMsgId);
            return;
        }

        var linkOpt = linkedChannelRepository.findByDiscordChannelIdAndIsActiveTrue(channelId);
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

    /** Syncs edits from Discord back to the platform post content. */
    @Transactional
    public void handleDiscordMessageUpdated(DiscordMessageUpdatedPayload payload) {
        String discordMsgId = payload.discordMsgId();
        String content = payload.content();

        messageSyncRepository.findByDiscordMessageId(discordMsgId).ifPresent(sync -> {
            projectPostRepository.findById(sync.getPlatformPostId()).ifPresent(post -> {
                post.setContent(content != null ? content : "");
                projectPostRepository.save(post);
                log.info("Post updated from discord: postId={}", post.getId());
            });
        });
    }

    /** Removes the platform post when the Discord message is deleted. */
    @Transactional
    public void handleDiscordMessageDeleted(DiscordMessageDeletedPayload payload) {
        String discordMsgId = payload.discordMsgId();

        messageSyncRepository.findByDiscordMessageId(discordMsgId).ifPresent(sync -> {
            projectPostRepository.findById(sync.getPlatformPostId()).ifPresent(post -> {
                if (post.getStatus() == ProjectPostStatus.ARCHIVED) {
                    log.debug("Discord message deleted for archived post {} — removing sync only", post.getId());
                } else {
                    projectPostRepository.delete(post);
                    log.info("Post deleted from discord sync: postId={}", post.getId());
                }
            });
            messageSyncRepository.delete(sync);
        });
    }

    @Transactional
    public void handleMessageAssigned(MessageAssignedPayload payload) {
        UUID postUuid = payload.postId();
        String discordMsgId = payload.discordMsgId();
        String channelId = payload.channelId();
        String guildId = payload.guildId();

        if (messageSyncRepository.existsByDiscordMessageId(discordMsgId)) {
            return;
        }

        if (!projectPostRepository.existsById(postUuid)) {
            log.warn("Post {} not found in this DB — skipping cross-environment MESSAGE_ASSIGNED", postUuid);
            return;
        }

        messageSyncRepository.findByPlatformPostId(postUuid)
                .ifPresent(sync -> messageSyncRepository.delete(sync));

        messageSyncRepository.save(DiscordMessageSyncEntity.builder()
                .platformPostId(postUuid)
                .discordMessageId(discordMsgId)
                .discordChannelId(channelId)
                .discordGuildId(guildId)
                .direction(DiscordMessageSyncEntity.SyncDirection.PLATFORM_TO_DISCORD)
                .build());
        log.info("Message assigned: postId={}, discordMsgId={}", postUuid, discordMsgId);
    }

    /** Logs the user's response to a project invitation (accept / decline). */
    @Transactional
    public void handleInviteResponse(InviteResponsePayload payload) {
        log.info("Invite response received: inviteId={}, response={}", payload.inviteId(), payload.response());
    }

    /** Marks the channel link as inactive when the bot loses access (kick / ban / channel delete). */
    @Transactional
    public void handleChannelDisconnected(ChannelDisconnectedPayload payload) {
        String channelId = payload.channelId();

        linkedChannelRepository.findByDiscordChannelId(channelId).ifPresent(link -> {
            link.setActive(false);
            linkedChannelRepository.save(link);
            log.warn("Channel disconnected: channelId={}, reason={}", channelId, payload.reason());
        });
    }

}
