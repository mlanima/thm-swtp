package de.thm.swtp.api.discord.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.thm.swtp.api.discord.config.DiscordProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
        try {
            return restClient.post()
                    .uri(discordProperties.getBot().getBaseUrl() + "/internal/test-connection")
                    .headers(h -> h.addAll(authHeaders()))
                    .body(Map.of("channelId", channelId))
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TestConnectionResponse(boolean success, String reason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RetryJobResponse(boolean success) {}
}
