package de.thm.swtp.api.github.client;

import de.thm.swtp.api.github.exception.GithubApiException;
import de.thm.swtp.api.github.exception.GithubRepoNotFoundException;
import de.thm.swtp.api.github.exception.GithubTokenInvalidException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GithubApiClientTest {

    private GithubApiClient clientWithResponse(final int statusCode, final String body) {
        ClientHttpRequestInterceptor interceptor = (request, bodyBytes, execution) -> {
            var response = new MockClientHttpResponse(
                    body.getBytes(StandardCharsets.UTF_8), HttpStatus.valueOf(statusCode));
            response.getHeaders().set("Content-Type", "application/json");
            return response;
        };
        var restClient = RestClient.builder()
                .baseUrl("https://test.github.com")
                .requestInterceptor(interceptor)
                .build();
        return new GithubApiClient(restClient);
    }

    @Test
    void shouldReturnAuthenticatedUser() {
        var json = """
                {"id": 1, "login": "octocat", "name": "The Octocat", "avatar_url": "https://avatars.example/1"}
                """;
        var user = clientWithResponse(200, json).getAuthenticatedUser("gho_token");
        assertThat(user.login()).isEqualTo("octocat");
        assertThat(user.id()).isEqualTo(1L);
    }

    @Test
    void shouldThrowTokenInvalidWhenUnauthorized() {
        assertThatThrownBy(() -> clientWithResponse(401, "Unauthorized").getAuthenticatedUser("bad-token"))
                .isInstanceOf(GithubTokenInvalidException.class);
    }

    @Test
    void shouldReturnRepository() {
        var json = """
                {"id": 42, "name": "thm-swtp", "full_name": "mlanima/thm-swtp",
                 "description": "IdeaCamp", "html_url": "https://github.com/mlanima/thm-swtp",
                 "private": false, "stargazers_count": 7, "forks_count": 2,
                 "permissions": {"admin": false, "push": true, "pull": true}}
                """;
        var repo = clientWithResponse(200, json).getRepository("gho_token", "mlanima", "thm-swtp");
        assertThat(repo.fullName()).isEqualTo("mlanima/thm-swtp");
        assertThat(repo.isPrivate()).isFalse();
        assertThat(repo.stargazersCount()).isEqualTo(7);
        assertThat(repo.hasWriteAccess()).isTrue();
    }

    @Test
    void shouldNotHaveWriteAccessWhenPermissionsAreAllFalse() {
        var json = """
                {"id": 42, "name": "thm-swtp", "full_name": "mlanima/thm-swtp",
                 "description": "IdeaCamp", "html_url": "https://github.com/mlanima/thm-swtp",
                 "private": false, "stargazers_count": 7, "forks_count": 2,
                 "permissions": {"admin": false, "push": false, "pull": true}}
                """;
        var repo = clientWithResponse(200, json).getRepository("gho_token", "mlanima", "thm-swtp");
        assertThat(repo.hasWriteAccess()).isFalse();
    }

    @Test
    void shouldNotHaveWriteAccessWhenPermissionsAreAbsent() {
        var json = """
                {"id": 42, "name": "thm-swtp", "full_name": "mlanima/thm-swtp",
                 "description": "IdeaCamp", "html_url": "https://github.com/mlanima/thm-swtp",
                 "private": false, "stargazers_count": 7, "forks_count": 2}
                """;
        var repo = clientWithResponse(200, json).getRepository("gho_token", "mlanima", "thm-swtp");
        assertThat(repo.hasWriteAccess()).isFalse();
    }

    @Test
    void shouldThrowNotFoundWhenRepoMissing() {
        assertThatThrownBy(() -> clientWithResponse(404, "Not Found")
                .getRepository("gho_token", "mlanima", "missing-repo"))
                .isInstanceOf(GithubRepoNotFoundException.class);
    }

    @Test
    void shouldThrowApiExceptionOnServerError() {
        assertThatThrownBy(() -> clientWithResponse(500, "Internal Server Error")
                .getRepository("gho_token", "mlanima", "thm-swtp"))
                .isInstanceOf(GithubApiException.class);
    }

    @Test
    void shouldReturnLanguages() {
        var json = """
                {"Java": 120000, "TypeScript": 45000}
                """;
        var languages = clientWithResponse(200, json).getRepositoryLanguages("gho_token", "mlanima", "thm-swtp");
        assertThat(languages).containsEntry("Java", 120000L).containsEntry("TypeScript", 45000L);
    }

    @Test
    void shouldReturnEmptyMapWhenLanguagesResponseIsNull() {
        var languages = clientWithResponse(200, "null").getRepositoryLanguages("gho_token", "mlanima", "thm-swtp");
        assertThat(languages).isEqualTo(Map.of());
    }
}
