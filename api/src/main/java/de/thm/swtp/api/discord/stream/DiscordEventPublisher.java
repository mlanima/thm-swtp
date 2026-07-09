package de.thm.swtp.api.discord.stream;

import de.thm.swtp.api.discord.config.DiscordProperties;
import de.thm.swtp.api.discord.entity.DiscordChannelSettingsEntity;
import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import de.thm.swtp.api.discord.repository.DiscordChannelSettingsRepository;
import de.thm.swtp.api.discord.repository.DiscordMessageSyncRepository;
import de.thm.swtp.api.discord.repository.LinkedChannelRepository;
import de.thm.swtp.api.projectPost.entity.ProjectPostEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiscordEventPublisher {

    private final StringRedisTemplate redis;
    private final DiscordProperties discordProperties;
    private final LinkedChannelRepository linkedChannelRepository;
    private final DiscordMessageSyncRepository messageSyncRepository;
    private final DiscordChannelSettingsRepository settingsRepository;
    private final ObjectMapper objectMapper;

    public void publishPostCreated(ProjectPostEntity post) {
        var channelRef = findActiveChannel(post.getProject().getId());
        if (channelRef.isEmpty()) {
            return;
        }

        var link = channelRef.get();
        String content = post.getContent();
        if (content.length() > 3900) {
            content = content.substring(0, 3897) + "...";
        }

        var payload = new HashMap<String, String>();
        payload.put("postId", post.getId().toString());
        payload.put("projectId", post.getProject().getId().toString());
        payload.put("channelId", link.getDiscordChannelId());
        payload.put("content", content);
        payload.put("title", post.getTitle());
        payload.put("authorName", post.getAuthor().getUsername());
        payload.put("platformUrl", buildPostUrl(post));

        var authorAvatar = post.getAuthor().getDiscordAvatar();
        if (authorAvatar != null && !authorAvatar.isBlank()) {
            payload.put("authorAvatar", authorAvatar);
        }

        send("POST_CREATED", payload);
    }

    public void publishPostUpdated(ProjectPostEntity post, String discordMsgId) {
        var channelRef = findActiveChannel(post.getProject().getId());
        if (channelRef.isEmpty()) {
            return;
        }

        String content = post.getContent();
        if (content.length() > 3900) {
            content = content.substring(0, 3897) + "...";
        }

        send("POST_UPDATED", Map.of(
                "postId", post.getId().toString(),
                "discordMsgId", discordMsgId,
                "channelId", channelRef.get().getDiscordChannelId(),
                "content", content
        ));
    }

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

    public void publishDirectDelete(UUID postId, String discordMsgId, String channelId) {
        send("POST_DELETED", Map.of(
                "postId", postId.toString(),
                "discordMsgId", discordMsgId,
                "channelId", channelId
        ));
        log.info("Discord direct delete sent: postId={}, discordMsgId={}", postId, discordMsgId);
    }

    public void publishProjectInvite(UUID inviteId, String targetDiscordId, String projectName, String inviterName) {
        send("PROJECT_INVITE", Map.of(
                "inviteId", inviteId.toString(),
                "targetDiscordId", targetDiscordId,
                "projectName", projectName,
                "inviterName", inviterName
        ));
    }

    public void publishProjectEvent(UUID projectId, String eventType, String message) {
        var channelRef = findActiveChannel(projectId);
        if (channelRef.isEmpty()) {
            return;
        }

        var link = channelRef.get();
        var settings = settingsRepository.findByLinkedChannelId(link.getId());

        if (settings.isEmpty() || !shouldNotify(eventType, settings.get())) {
            return;
        }

        send("PROJECT_EVENT", Map.of(
                "eventType", eventType,
                "projectId", projectId.toString(),
                "projectName", link.getProject().getName(),
                "channelId", link.getDiscordChannelId(),
                "message", message.length() > 3900 ? message.substring(0, 3897) + "..." : message
        ));
    }

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

    private Optional<LinkedChannelEntity> findActiveChannel(UUID projectId) {
        return linkedChannelRepository.findByProjectId(projectId)
                .filter(LinkedChannelEntity::isActive);
    }

    private boolean shouldNotify(String eventType, DiscordChannelSettingsEntity settings) {
        return switch (eventType) {
            case "MEMBER_JOIN" -> settings.isNotifyMemberJoin();
            case "MEMBER_LEAVE" -> settings.isNotifyMemberLeave();
            default -> true;
        };
    }

    private String buildPostUrl(ProjectPostEntity post) {
        return "https://swtp-ss26.de/project/" + post.getProject().getProjectUrl();
    }
}
