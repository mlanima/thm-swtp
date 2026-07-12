package de.thm.swtp.api.discord.client;

import de.thm.swtp.api.discord.config.DiscordProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.function.Supplier;

/**
 * REST client that talks to the internal Discord bot service.
 * Every request is authenticated with a shared secret header and falls back to a
 * default error response when the bot is unreachable.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BotInternalClient implements BotOperations {

    private final DiscordProperties discordProperties;

    private final RestClient restClient = RestClient.builder().build();

    private String baseUrl() {
        return discordProperties.getBot().getBaseUrl();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-internal-secret", discordProperties.getBot().getApiSecret());
        return headers;
    }

    /**
     * Runs a bot API call and returns the given fallback if anything goes wrong.
     * This keeps the caller from having to handle transport errors every time.
     */
    private <T> T callBot(Supplier<T> call, T fallback) {
        try {
            return call.get();
        } catch (Exception e) {
            log.error("Bot request failed: {}", e.getMessage());
            return fallback;
        }
    }

    @Override
    public TestConnectionResponse testConnection(String channelId) {
        return testConnection(channelId, null);
    }

    /**
     * Pings the bot to verify it can see the specified channel (and optionally guild).
     */
    @Override
    public TestConnectionResponse testConnection(String channelId, String guildId) {
        var body = guildId != null
                ? Map.of("channelId", channelId, "guildId", guildId)
                : Map.of("channelId", channelId);
        return callBot(
                () -> restClient.post()
                        .uri(baseUrl() + "/internal/test-connection")
                        .headers(h -> h.addAll(authHeaders()))
                        .body(body)
                        .retrieve()
                        .body(TestConnectionResponse.class),
                new TestConnectionResponse(false, "bot unreachable")
        );
    }

    @Override
    public RetryJobResponse retryJob(String jobId) {
        return callBot(
                () -> restClient.post()
                        .uri(baseUrl() + "/internal/jobs/{jobId}/retry", jobId)
                        .headers(h -> h.addAll(authHeaders()))
                        .retrieve()
                        .body(RetryJobResponse.class),
                new RetryJobResponse(false)
        );
    }

    @Override
    public CreateInviteResponse createChannelInvite(String channelId) {
        return callBot(
                () -> restClient.post()
                        .uri(baseUrl() + "/internal/channels/{channelId}/invite", channelId)
                        .headers(h -> h.addAll(authHeaders()))
                        .retrieve()
                        .body(CreateInviteResponse.class),
                new CreateInviteResponse(false, null, "bot unreachable")
        );
    }

    @Override
    public LeaveGuildResponse leaveGuild(String channelId) {
        return callBot(
                () -> restClient.post()
                        .uri(baseUrl() + "/internal/channels/{channelId}/leave-guild", channelId)
                        .headers(h -> h.addAll(authHeaders()))
                        .retrieve()
                        .body(LeaveGuildResponse.class),
                new LeaveGuildResponse(false, "bot unreachable")
        );
    }

    @Override
    public RestrictChannelResponse restrictChannel(String channelId, String ownerDiscordId) {
        return callBot(
                () -> restClient.post()
                        .uri(baseUrl() + "/internal/channels/{channelId}/restrict", channelId)
                        .headers(h -> h.addAll(authHeaders()))
                        .body(Map.of("ownerDiscordId", ownerDiscordId))
                        .retrieve()
                        .body(RestrictChannelResponse.class),
                new RestrictChannelResponse(false, "bot unreachable")
        );
    }

    @Override
    public AutoSetupResponse autoSetup(String ownerDiscordId) {
        return autoSetup(ownerDiscordId, null);
    }

    /**
     * Tells the bot to create a text channel and set it up for the given owner.
     * Guild ID is optional — without it the bot picks the first guild it can write to.
     */
    @Override
    public AutoSetupResponse autoSetup(String ownerDiscordId, String guildId) {
        var body = guildId != null
                ? Map.of("ownerDiscordId", ownerDiscordId, "guildId", guildId)
                : Map.of("ownerDiscordId", ownerDiscordId);
        return callBot(
                () -> restClient.post()
                        .uri(baseUrl() + "/internal/auto-setup")
                        .headers(h -> h.addAll(authHeaders()))
                        .body(body)
                        .retrieve()
                        .body(AutoSetupResponse.class),
                new AutoSetupResponse(false, null, null, null, "bot unreachable", true)
        );
    }

    @Override
    public GuildsResponse getGuilds() {
        return callBot(
                () -> restClient.get()
                        .uri(baseUrl() + "/internal/guilds")
                        .headers(h -> h.addAll(authHeaders()))
                        .retrieve()
                        .body(GuildsResponse.class),
                new GuildsResponse(null, false, "bot unreachable")
        );
    }
}
