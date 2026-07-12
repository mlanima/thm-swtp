package de.thm.swtp.api.discord.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.thm.swtp.api.discord.config.DiscordProperties;
import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import de.thm.swtp.api.discord.repository.DiscordChannelSettingsRepository;
import de.thm.swtp.api.discord.repository.DiscordMessageSyncRepository;
import de.thm.swtp.api.discord.repository.LinkedChannelRepository;
import de.thm.swtp.api.projectPost.entity.ProjectPostEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Pushes domain events into the outbound Redis stream so the Discord bot
 * can pick them up and act on them (post messages, delete, notify, etc.).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DiscordEventPublisher {

    private static final int DISCORD_CONTENT_MAX = 3900;

    private final StringRedisTemplate redis;
    private final DiscordProperties discordProperties;
    private final LinkedChannelRepository linkedChannelRepository;
    private final DiscordMessageSyncRepository messageSyncRepository;
    private final DiscordChannelSettingsRepository settingsRepository;
    private final PostUrlBuilder postUrlBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Fires a POST_CREATED event so the bot sends a Discord message for the new post. */
    public void publishPostCreated(ProjectPostEntity post) {
        var channelRef = findActiveChannel(post.getProject().getId());
        if (channelRef.isEmpty()) {
            return;
        }

        var link = channelRef.get();
        var content = truncate(post.getContent());

        var payload = new HashMap<String, String>();
        payload.put("postId", post.getId().toString());
        payload.put("projectId", post.getProject().getId().toString());
        payload.put("channelId", link.getDiscordChannelId());
        payload.put("content", content);
        payload.put("title", post.getTitle());
        payload.put("authorName", post.getAuthor().getUsername());
        payload.put("platformUrl", postUrlBuilder.buildPostUrl(post));

        var authorAvatar = post.getAuthor().getDiscordAvatar();
        if (authorAvatar != null && !authorAvatar.isBlank()) {
            payload.put("authorAvatar", authorAvatar);
        }

        send("POST_CREATED", payload);
    }

    /** Fires a POST_UPDATED event so the bot edits the Discord message content. */
    public void publishPostUpdated(ProjectPostEntity post, String discordMsgId) {
        var channelRef = findActiveChannel(post.getProject().getId());
        if (channelRef.isEmpty()) {
            return;
        }

        var content = truncate(post.getContent());

        send("POST_UPDATED", Map.of(
                "postId", post.getId().toString(),
                "discordMsgId", discordMsgId,
                "channelId", channelRef.get().getDiscordChannelId(),
                "content", content
        ));
    }

    /** Fires a POST_DELETED event — looks up the sync record first, then tells the bot to delete. */
    public void publishPostDeleted(UUID postId, String discordMsgId) {
        messageSyncRepository.findByDiscordMessageId(discordMsgId).ifPresentOrElse(
            sync -> {
                send("POST_DELETED", Map.of(
                        "postId", postId.toString(),
                        "discordMsgId", sync.getDiscordMessageId(),
                        "channelId", sync.getDiscordChannelId()
                ));
                log.info("Discord delete event sent: postId={}, discordMsgId={}", postId, discordMsgId);
            },
            () -> log.info("Discord delete skipped — no sync record found: postId={}, discordMsgId={}", postId, discordMsgId)
        );
    }

    /** Skips the sync-record lookup and tells the bot to delete directly (used for orphan cleanup). */
    public void publishDirectDelete(UUID postId, String discordMsgId, String channelId) {
        send("POST_DELETED", Map.of(
                "postId", postId.toString(),
                "discordMsgId", discordMsgId,
                "channelId", channelId
        ));
        log.info("Discord direct delete sent: postId={}, discordMsgId={}", postId, discordMsgId);
    }

    /** Fires a PROJECT_INVITE event so the bot DMs the user about the invitation. */
    public void publishProjectInvite(UUID inviteId, String targetDiscordId, String projectName, String inviterName) {
        send("PROJECT_INVITE", Map.of(
                "inviteId", inviteId.toString(),
                "targetDiscordId", targetDiscordId,
                "projectName", projectName,
                "inviterName", inviterName
        ));
    }

    /** Fires a PROJECT_EVENT only if the channel's notification settings allow this event type. */
    public void publishProjectEvent(UUID projectId, String eventType, String message) {
        var channelRef = findActiveChannel(projectId);
        if (channelRef.isEmpty()) {
            return;
        }

        var link = channelRef.get();
        var settings = settingsRepository.findByLinkedChannelId(link.getId());

        if (settings.isEmpty() || !settings.get().shouldNotify(eventType)) {
            return;
        }

        send("PROJECT_EVENT", Map.of(
                "eventType", eventType,
                "projectId", projectId.toString(),
                "projectName", link.getProject().getName(),
                "channelId", link.getDiscordChannelId(),
                "message", truncate(message)
        ));
    }

    /** Serialises the payload to JSON and pushes it onto the outbound Redis stream. */
    private void send(String type, Map<String, ?> payload) {
        try {
            redis.opsForStream().add(
                    discordProperties.getStreams().getOutbound(),
                    Map.of("type", type, "payload", objectMapper.writeValueAsString(payload))
            );
            log.debug("Published {} event to stream {}", type, discordProperties.getStreams().getOutbound());
        } catch (Exception e) {
            log.error("Failed to publish {} event: {}", type, e.getMessage());
        }
    }

    /** Looks up the active channel link for the given project (null-safe). */
    private Optional<LinkedChannelEntity> findActiveChannel(UUID projectId) {
        return linkedChannelRepository.findByProjectId(projectId)
                .filter(LinkedChannelEntity::isActive);
    }

    /** Cuts text to DISCORD_CONTENT_MAX chars with an ellipsis — Discord has a 4000 char limit. */
    private static String truncate(String text) {
        if (text == null || text.length() <= DISCORD_CONTENT_MAX) {
            return text;
        }
        return text.substring(0, DISCORD_CONTENT_MAX - 3) + "...";
    }
}
