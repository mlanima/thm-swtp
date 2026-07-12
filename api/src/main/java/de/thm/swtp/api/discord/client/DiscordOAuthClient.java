package de.thm.swtp.api.discord.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.thm.swtp.api.discord.config.DiscordProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Handles the Discord OAuth2 flows — exchanging authorization codes for tokens
 * and fetching the authenticated user's profile.
 */
@Component
@Slf4j
public class DiscordOAuthClient {

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public DiscordOAuthClient(RestTemplate discordRestTemplate, DiscordProperties discordProperties) {
        this.restTemplate = discordRestTemplate;
        this.clientId = discordProperties.getOauth().getClientId();
        this.clientSecret = discordProperties.getOauth().getClientSecret();
        this.redirectUri = discordProperties.getOauth().getRedirectUri();
    }

    private static final String TOKEN_URL = "https://discord.com/api/oauth2/token";
    private static final String USER_URL = "https://discord.com/api/users/@me";

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BotTokenResponse(
            String accessToken,
            String scope,
            Guild guild
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Guild(String id, String name, @JsonProperty("owner_id") String ownerId) {}
    }

    /**
     * Exchanges the OAuth2 code from the bot-add flow for a BotTokenResponse.
     * Parses the raw JSON response manually because Discord returns extra fields
     * that would trip up automatic deserialization into our record.
     */
    public BotTokenResponse exchangeBotCode(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        var rawResponse = restTemplate.postForEntity(TOKEN_URL, request, String.class);
        if (!rawResponse.getStatusCode().is2xxSuccessful() || rawResponse.getBody() == null) {
            log.warn("Discord bot OAuth2 token exchange failed: {}", rawResponse.getStatusCode());
            throw new RuntimeException("Failed to exchange Discord OAuth2 code for bot");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            BotTokenResponse tokenResp = mapper.readValue(rawResponse.getBody(), BotTokenResponse.class);

            if (tokenResp.guild() == null || tokenResp.guild().id() == null) {
                log.warn("Discord token response missing guild object: {}", rawResponse.getBody());
                throw new RuntimeException("Discord token exchange did not return guild info");
            }

            log.info("Bot code exchanged successfully: guildId={}, guildName={}, ownerId={}",
                    tokenResp.guild().id(), tokenResp.guild().name(), tokenResp.guild().ownerId());
            return tokenResp;
        } catch (Exception e) {
            log.warn("Failed to parse Discord token response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse Discord token response", e);
        }
    }

    /**
     * Exchanges a user OAuth2 code for a raw token map.
     * Used for the standard Discord login flow (not bot installation).
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map<String, Object> exchangeCode(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(TOKEN_URL, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.warn("Discord OAuth2 token exchange failed: {}", response.getStatusCode());
            throw new RuntimeException("Failed to exchange Discord OAuth2 code");
        }

        return response.getBody();
    }

    /**
     * Fetches the Discord user profile associated with the given access token.
     * Returns a raw map with fields like {@code id}, {@code username}, {@code avatar}.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map<String, Object> getUser(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(USER_URL, HttpMethod.GET, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.warn("Discord /users/@me failed: {}", response.getStatusCode());
            throw new RuntimeException("Failed to fetch Discord user");
        }

        return response.getBody();
    }
}
