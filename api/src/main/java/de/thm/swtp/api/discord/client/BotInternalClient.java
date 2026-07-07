package de.thm.swtp.api.discord.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.thm.swtp.api.discord.config.DiscordProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BotInternalClient {

    private final DiscordProperties discordProperties;

    private final RestClient restClient = RestClient.builder().build();

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-internal-secret", discordProperties.getBot().getApiSecret());
        return headers;
    }

    public TestConnectionResponse testConnection(String channelId) {
        return testConnection(channelId, null);
    }

    public TestConnectionResponse testConnection(String channelId, String guildId) {
        try {
            var body = guildId != null
                    ? Map.of("channelId", channelId, "guildId", guildId)
                    : Map.of("channelId", channelId);
            return restClient.post()
                    .uri(discordProperties.getBot().getBaseUrl() + "/internal/test-connection")
                    .headers(h -> h.addAll(authHeaders()))
                    .body(body)
                    .retrieve()
                    .body(TestConnectionResponse.class);
        } catch (Exception e) {
            log.error("Bot test-connection failed for channel {}: {}", channelId, e.getMessage());
            return new TestConnectionResponse(false, "bot unreachable");
        }
    }

    public RetryJobResponse retryJob(String jobId) {
        try {
            return restClient.post()
                    .uri(discordProperties.getBot().getBaseUrl() + "/internal/jobs/{jobId}/retry", jobId)
                    .headers(h -> h.addAll(authHeaders()))
                    .retrieve()
                    .body(RetryJobResponse.class);
        } catch (Exception e) {
            log.error("Bot retry-job failed for job {}: {}", jobId, e.getMessage());
            return new RetryJobResponse(false);
        }
    }

    public CreateInviteResponse createChannelInvite(String channelId) {
        try {
            return restClient.post()
                    .uri(discordProperties.getBot().getBaseUrl() + "/internal/channels/{channelId}/invite", channelId)
                    .headers(h -> h.addAll(authHeaders()))
                    .retrieve()
                    .body(CreateInviteResponse.class);
        } catch (Exception e) {
            log.error("Bot create-invite failed for channel {}: {}", channelId, e.getMessage());
            return new CreateInviteResponse(false, null, "bot unreachable");
        }
    }

    public LeaveGuildResponse leaveGuild(String channelId) {
        try {
            return restClient.post()
                    .uri(discordProperties.getBot().getBaseUrl() + "/internal/channels/{channelId}/leave-guild", channelId)
                    .headers(h -> h.addAll(authHeaders()))
                    .retrieve()
                    .body(LeaveGuildResponse.class);
        } catch (Exception e) {
            log.error("Bot leave-guild failed for channel {}: {}", channelId, e.getMessage());
            return new LeaveGuildResponse(false, "bot unreachable");
        }
    }

    public RestrictChannelResponse restrictChannel(String channelId, String ownerDiscordId) {
        try {
            return restClient.post()
                    .uri(discordProperties.getBot().getBaseUrl() + "/internal/channels/{channelId}/restrict", channelId)
                    .headers(h -> h.addAll(authHeaders()))
                    .body(Map.of("ownerDiscordId", ownerDiscordId))
                    .retrieve()
                    .body(RestrictChannelResponse.class);
        } catch (Exception e) {
            log.error("Bot restrict-channel failed for channel {}: {}", channelId, e.getMessage());
            return new RestrictChannelResponse(false, "bot unreachable");
        }
    }

    public AutoSetupResponse autoSetup(String ownerDiscordId) {
        return autoSetup(ownerDiscordId, null);
    }

    public AutoSetupResponse autoSetup(String ownerDiscordId, String guildId) {
        try {
            var body = guildId != null
                    ? Map.of("ownerDiscordId", ownerDiscordId, "guildId", guildId)
                    : Map.of("ownerDiscordId", ownerDiscordId);
            return restClient.post()
                    .uri(discordProperties.getBot().getBaseUrl() + "/internal/auto-setup")
                    .headers(h -> h.addAll(authHeaders()))
                    .body(body)
                    .retrieve()
                    .body(AutoSetupResponse.class);
        } catch (Exception e) {
            log.error("Bot auto-setup failed: {}", e.getMessage());
            return new AutoSetupResponse(false, null, null, null, "bot unreachable");
        }
    }

    public GuildsResponse getGuilds() {
        try {
            return restClient.get()
                    .uri(discordProperties.getBot().getBaseUrl() + "/internal/guilds")
                    .headers(h -> h.addAll(authHeaders()))
                    .retrieve()
                    .body(GuildsResponse.class);
        } catch (Exception e) {
            log.error("Bot get-guilds failed: {}", e.getMessage());
            return new GuildsResponse(null, false, "bot unreachable");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GuildInfo(String id, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GuildsResponse(List<GuildInfo> guilds, boolean success, String reason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TestConnectionResponse(boolean success, String reason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RetryJobResponse(boolean success) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CreateInviteResponse(boolean success, String inviteUrl, String reason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AutoSetupResponse(boolean success, String guildId, String channelId, String channelName, String reason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LeaveGuildResponse(boolean success, String reason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RestrictChannelResponse(boolean success, String reason) {}
}
