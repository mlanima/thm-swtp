package de.thm.swtp.api.github.client;

import de.thm.swtp.api.github.config.GithubOAuthProperties;
import de.thm.swtp.api.github.exception.GithubOAuthException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GithubOAuthClientTest {

    private static final GithubOAuthProperties PROPERTIES = new GithubOAuthProperties(
            "client-id", "client-secret", "http://localhost:4200/github/callback",
            "read:user", "https://github.com/login/oauth/authorize",
            "https://github.com/login/oauth/access_token", "test-key");

    private GithubOAuthClient clientWithResponse(final int statusCode, final String body) {
        ClientHttpRequestInterceptor interceptor = (request, bodyBytes, execution) -> {
            var response = new MockClientHttpResponse(
                    body.getBytes(StandardCharsets.UTF_8), HttpStatus.valueOf(statusCode));
            response.getHeaders().set("Content-Type", "application/json");
            return response;
        };
        var restClient = RestClient.builder().requestInterceptor(interceptor).build();
        return new GithubOAuthClient(restClient, PROPERTIES, "https://api.github.com");
    }

    @Test
    void shouldReturnAccessTokenOnSuccess() {
        var json = """
                {"access_token": "gho_abc123", "scope": "read:user", "token_type": "bearer"}
                """;
        var result = clientWithResponse(200, json).exchangeCode("valid-code");
        assertThat(result.accessToken()).isEqualTo("gho_abc123");
        assertThat(result.scope()).isEqualTo("read:user");
    }

    @Test
    void shouldThrowWhenGithubReturnsErrorField() {
        var json = """
                {"error": "bad_verification_code", "error_description": "The code passed is incorrect or expired."}
                """;
        assertThatThrownBy(() -> clientWithResponse(200, json).exchangeCode("bad-code"))
                .isInstanceOf(GithubOAuthException.class);
    }

    @Test
    void shouldThrowWhenServerError() {
        assertThatThrownBy(() -> clientWithResponse(500, "Internal Server Error").exchangeCode("any-code"))
                .isInstanceOf(GithubOAuthException.class);
    }

    @Test
    void shouldThrowWhenAccessTokenMissing() {
        var json = """
                {"scope": "read:user", "token_type": "bearer"}
                """;
        assertThatThrownBy(() -> clientWithResponse(200, json).exchangeCode("valid-code"))
                .isInstanceOf(GithubOAuthException.class);
    }

    @Test
    void shouldNotThrowWhenRevocationSucceeds() {
        assertThatCode(() -> clientWithResponse(204, "").revokeGrant("gho_abc123"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldSwallowErrorsWhenRevocationFails() {
        assertThatCode(() -> clientWithResponse(404, "Not Found").revokeGrant("gho_abc123"))
                .doesNotThrowAnyException();
    }
}
