package de.thm.swtp.api.discord.stream;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import de.thm.swtp.api.discord.config.DiscordProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformSyncConsumer {

    private final StringRedisTemplate redis;
    private final DiscordProperties discordProperties;
    private final ObjectMapper objectMapper;
    private final DiscordEventHandler eventHandler;

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

        ensureGroup();

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

    private void ensureGroup() {
        var inbound = discordProperties.getStreams().getInbound();
        var group = discordProperties.getStreams().getConsumerGroup();
        try {
            redis.opsForStream().createGroup(inbound, group);
            log.info("Created consumer group {} on stream {}", group, inbound);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.debug("Consumer group already exists");
            } else {
                try {
                    redis.opsForStream().add(inbound, Map.of("_init", "1"));
                    redis.opsForStream().createGroup(inbound, group);
                    log.info("Created consumer group {} on stream {} (stream auto-created)", group, inbound);
                } catch (Exception e2) {
                    if (e2.getMessage() != null && e2.getMessage().contains("BUSYGROUP")) {
                        log.debug("Consumer group already exists");
                    } else {
                        log.warn("Failed to create consumer group: {}", e2.getMessage());
                    }
                }
            }
        }
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
            if (e.getMessage() != null && e.getMessage().contains("NOGROUP")) {
                log.warn("Consumer group missing — attempting to recreate");
                ensureGroup();
            } else {
                log.debug("Stream poll error: {}", e.getMessage());
            }
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
            EventType eventType;

            try {
                eventType = EventType.valueOf(type);
            } catch (IllegalArgumentException e) {
                log.warn("Unknown event type: {}", type);
                return;
            }

            switch (eventType) {
                case DISCORD_MESSAGE_CREATED -> eventHandler.handleDiscordMessageCreated(payload);
                case DISCORD_MESSAGE_UPDATED -> eventHandler.handleDiscordMessageUpdated(payload);
                case DISCORD_MESSAGE_DELETED -> eventHandler.handleDiscordMessageDeleted(payload);
                case DISCORD_MESSAGE_ASSIGNED -> eventHandler.handleMessageAssigned(payload);
                case INVITE_RESPONSE -> eventHandler.handleInviteResponse(payload);
                case CHANNEL_DISCONNECTED -> eventHandler.handleChannelDisconnected(payload);
            }
        } catch (Exception e) {
            log.error("Error processing event {}: {}", type, e.getMessage());
        }
    }
}
