package de.thm.swtp.api.discord.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
public class DiscordOAuthClient {

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public DiscordOAuthClient(RestTemplate discordRestTemplate,
                              @Value("${DISCORD_CLIENT_ID:}") String clientId,
                              @Value("${DISCORD_CLIENT_SECRET:}") String clientSecret,
                              @Value("${DISCORD_REDIRECT_URI:}") String redirectUri) {
        this.restTemplate = discordRestTemplate;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    private static final String TOKEN_URL = "https://discord.com/api/oauth2/token";
    private static final String USER_URL = "https://discord.com/api/users/@me";

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
