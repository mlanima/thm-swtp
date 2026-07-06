package de.thm.swtp.api.github.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.thm.swtp.api.common.LogSafe;
import de.thm.swtp.api.github.config.GithubOAuthProperties;
import de.thm.swtp.api.github.exception.GithubOAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Talks to github.com (not api.github.com) for the OAuth code-exchange and best-effort grant
 * revocation on disconnect. */
@Slf4j
@Component
public class GithubOAuthClient {

    private final RestClient restClient;
    private final GithubOAuthProperties properties;
    private final String apiBaseUrl;

    @Autowired
    public GithubOAuthClient(
            final GithubOAuthProperties properties,
            @Value("${github.api.base-url:https://api.github.com}") final String apiBaseUrl) {
        this(buildClient(), properties, apiBaseUrl);
    }

    GithubOAuthClient(final RestClient restClient, final GithubOAuthProperties properties, final String apiBaseUrl) {
        this.restClient = restClient;
        this.properties = properties;
        this.apiBaseUrl = apiBaseUrl;
    }

    private static RestClient buildClient() {
        return RestClient.builder()
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor((request, body, execution) -> {
                    log.debug("GitHub OAuth request: {}", request.getURI());
                    return execution.execute(request, body);
                })
                .build();
    }

    public AccessTokenResult exchangeCode(final String code) {
        var response = restClient.post()
                .uri(properties.tokenUrl())
                .body(new AccessTokenRequest(
                        properties.clientId(), properties.clientSecret(), code, properties.redirectUri()))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        (request, res) -> {
                            log.debug("GitHub OAuth token exchange returned {}", res.getStatusCode());
                            throw new GithubOAuthException("Failed to exchange authorization code with GitHub");
                        })
                .body(AccessTokenResult.class);

        if (response == null) {
            throw new GithubOAuthException("GitHub OAuth token exchange returned an empty response");
        }
        if (response.error() != null) {
            log.debug("GitHub OAuth token exchange error: {}", LogSafe.clean(response.error()));
            throw new GithubOAuthException("GitHub rejected the authorization code");
        }
        if (response.accessToken() == null || response.accessToken().isBlank()) {
            throw new GithubOAuthException("GitHub OAuth token exchange returned no access token");
        }
        return response;
    }

    /** Best-effort: revoking the OAuth grant is a courtesy on disconnect, not a hard requirement. */
    public void revokeGrant(final String accessToken) {
        try {
            restClient.method(HttpMethod.DELETE)
                    .uri(apiBaseUrl + "/applications/{clientId}/grant", properties.clientId())
                    .headers(headers -> headers.setBasicAuth(properties.clientId(), properties.clientSecret()))
                    .body(new RevokeGrantRequest(accessToken))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, res) -> {
                                throw new GithubOAuthException("GitHub grant revocation returned " + res.getStatusCode());
                            })
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            log.warn("GitHub grant revocation failed (best-effort, ignoring): {}", e.getMessage());
        }
    }

    public record AccessTokenResult(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("scope") String scope,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("error") String error,
            @JsonProperty("error_description") String errorDescription) {}

    private record AccessTokenRequest(
            @JsonProperty("client_id") String clientId,
            @JsonProperty("client_secret") String clientSecret,
            @JsonProperty("code") String code,
            @JsonProperty("redirect_uri") String redirectUri) {}

    private record RevokeGrantRequest(@JsonProperty("access_token") String accessToken) {}
}
