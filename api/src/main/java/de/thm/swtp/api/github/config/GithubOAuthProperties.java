package de.thm.swtp.api.github.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github.oauth")
public record GithubOAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String scopes,
        String authorizeUrl,
        String tokenUrl,
        String tokenEncryptionKey) {

    public boolean enabled() {
        return !isBlank(clientId) && !isBlank(clientSecret) && !isBlank(tokenEncryptionKey);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
