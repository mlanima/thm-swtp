package de.thm.swtp.api.discord.stream;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import de.thm.swtp.api.discord.config.DiscordProperties;
import de.thm.swtp.api.discord.entity.DiscordMessageSyncEntity;
import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import de.thm.swtp.api.discord.repository.DiscordMessageSyncRepository;
import de.thm.swtp.api.discord.repository.LinkedChannelRepository;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.projectPost.domain.ProjectPostStatus;
import de.thm.swtp.api.projectPost.entity.ProjectPostEntity;
import de.thm.swtp.api.projectPost.repository.ProjectPostRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformSyncConsumer {

    private final StringRedisTemplate redis;
    private final DiscordProperties discordProperties;
    private final DiscordMessageSyncRepository messageSyncRepository;
    private final LinkedChannelRepository linkedChannelRepository;
    private final ProjectPostRepository projectPostRepository;
    private final UserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper;

    private String consumerName;

    @PostConstruct
    public void init() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            host = "unknown";
        }
        consumerName = "spring-worker-" + host + "-" + ProcessHandle.current().pid();

        try {
            redis.opsForStream().createGroup(
                    discordProperties.getStreams().getInbound(),
                    discordProperties.getStreams().getConsumerGroup()
            );
            log.info("Created consumer group {} on stream {}",
                    discordProperties.getStreams().getConsumerGroup(),
                    discordProperties.getStreams().getInbound());
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.debug("Consumer group already exists");
            } else {
                log.warn("Failed to create consumer group: {}", e.getMessage());
            }
        }

        var bot = discordProperties.getBot();
        var oauth = discordProperties.getOauth();
        if (bot.getApiSecret() == null || bot.getApiSecret().isBlank()) {
            log.warn("DISCORD_BOT_API_SECRET is not set — bot will reject API requests");
        }
        if (oauth.getClientId() == null || oauth.getClientId().isBlank()) {
            log.warn("DISCORD_CLIENT_ID is not set — Discord OAuth login will fail");
        }
        if (oauth.getClientSecret() == null || oauth.getClientSecret().isBlank()) {
            log.warn("DISCORD_CLIENT_SECRET is not set — Discord OAuth login will fail");
        }
        log.info("Discord config: bot={}, inbound={}, outbound={}",
                bot.getBaseUrl(),
                discordProperties.getStreams().getInbound(),
                discordProperties.getStreams().getOutbound());
    }

    @Scheduled(fixedDelay = 100)
    public void poll() {
        try {
            var records = redis.opsForStream().read(
                    Consumer.from(discordProperties.getStreams().getConsumerGroup(), consumerName),
                    StreamReadOptions.empty().count(discordProperties.getStreams().getBatchSize())
                            .block(Duration.ofMillis(discordProperties.getStreams().getBlockMs())),
                    StreamOffset.create(discordProperties.getStreams().getInbound(), ReadOffset.lastConsumed())
            );

            if (records == null) {
                return;
            }

            for (var record : records) {
                try {
                    processRecord(record.getId(), (Map<String, Object>) (Map<?, ?>) record.getValue());
                    redis.opsForStream().acknowledge(
                            discordProperties.getStreams().getInbound(),
                            discordProperties.getStreams().getConsumerGroup(),
                            record.getId()
                    );
                } catch (Exception e) {
                    log.error("Failed to process stream record {}: {}", record.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("Stream poll error (expected if empty): {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 30000)
    public void reclaimPending() {
        try {
            var pending = redis.opsForStream().pending(
                    discordProperties.getStreams().getInbound(),
                    discordProperties.getStreams().getConsumerGroup()
            );
            if (pending != null && pending.getTotalPendingMessages() > 0) {
                log.debug("Pending messages: {}", pending.getTotalPendingMessages());
            }
        } catch (Exception e) {
            log.warn("Failed to check pending messages: {}", e.getMessage());
        }
    }

    private void processRecord(RecordId recordId, Map<String, Object> rawFields) {
        Map<String, String> fields = new java.util.HashMap<>();
        rawFields.forEach((k, v) -> fields.put(k, v != null ? v.toString() : null));
        String type = fields.get("type");
        String payloadJson = fields.get("payload");

        if (type == null || payloadJson == null) {
            return;
        }

        try {
            Map<String, String> payload = objectMapper.readValue(payloadJson, new TypeReference<>() {});

            switch (type) {
                case "DISCORD_MESSAGE_CREATED" -> handleDiscordMessageCreated(payload);
                case "DISCORD_MESSAGE_UPDATED" -> handleDiscordMessageUpdated(payload);
                case "DISCORD_MESSAGE_DELETED" -> handleDiscordMessageDeleted(payload);
                case "DISCORD_MESSAGE_ASSIGNED" -> handleMessageAssigned(payload);
                case "INVITE_RESPONSE" -> handleInviteResponse(payload);
                case "CHANNEL_DISCONNECTED" -> handleChannelDisconnected(payload);
                default -> log.warn("Unknown event type: {}", type);
            }
        } catch (Exception e) {
            log.error("Error processing event {}: {}", type, e.getMessage());
        }
    }

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
        ProjectEntity project = link.getProject();

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

        DiscordMessageSyncEntity sync = DiscordMessageSyncEntity.builder()
                .platformPostId(UUID.fromString(postId))
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
