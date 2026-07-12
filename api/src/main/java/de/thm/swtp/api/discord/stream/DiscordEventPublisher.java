package de.thm.swtp.api.discord.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.thm.swtp.api.discord.config.DiscordProperties;
import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import de.thm.swtp.api.discord.repository.DiscordChannelSettingsRepository;
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
    private final DiscordChannelSettingsRepository settingsRepository;
    private final PostUrlBuilder postUrlBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Fires a POST_CREATED event so the bot sends a Discord message for the new post. */
    public void publishPostCreated(ProjectPostEntity post) {
        publishPostCreated(new PostCreatedPayload(
            post.getId(),
            post.getProject().getId(),
            post.getContent(),
            post.getTitle(),
            post.getAuthor().getUsername(),
            post.getAuthor().getDiscordAvatar(),
            postUrlBuilder.buildPostUrl(post)
        ));
    }

    /** Entity-data-safe overload for after-commit use (data extracted eagerly, no lazy access after commit). */
    public void publishPostCreated(PostCreatedPayload data) {
        var channelRef = findActiveChannel(data.projectId());
        if (channelRef.isEmpty()) {
            return;
        }

        var link = channelRef.get();
        var payload = new HashMap<String, String>();
        payload.put("postId", data.postId().toString());
        payload.put("projectId", data.projectId().toString());
        payload.put("channelId", link.getDiscordChannelId());
        payload.put("content", truncate(data.content()));
        payload.put("title", data.title());
        payload.put("authorName", data.authorName());
        payload.put("platformUrl", data.platformUrl());

        if (data.authorAvatar() != null && !data.authorAvatar().isBlank()) {
            payload.put("authorAvatar", data.authorAvatar());
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

    /** Fires a POST_DELETED event with the channelId provided directly (no sync-record lookup). */
    public void publishPostDeleted(UUID postId, String discordMsgId, String channelId) {
        send("POST_DELETED", Map.of(
                "postId", postId.toString(),
                "discordMsgId", discordMsgId,
                "channelId", channelId
        ));
        log.info("Discord delete event sent: postId={}, discordMsgId={}", postId, discordMsgId);
    }

    /** Tells the bot to delete a Discord message directly (alias for after-commit safety). */
    public void publishDirectDelete(UUID postId, String discordMsgId, String channelId) {
        publishPostDeleted(postId, discordMsgId, channelId);
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
