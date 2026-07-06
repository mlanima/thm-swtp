package de.thm.swtp.api.github.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.thm.swtp.api.github.exception.GithubApiException;
import de.thm.swtp.api.github.exception.GithubRepoNotFoundException;
import de.thm.swtp.api.github.exception.GithubTokenInvalidException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.function.Consumer;

/** Talks to api.github.com. Every method takes a nullable access token: null means an
 * unauthenticated call (public data only, subject to the stricter unauthenticated rate limit). */
@Slf4j
@Component
public class GithubApiClient {

    private static final String API_VERSION = "2022-11-28";

    private final RestClient restClient;

    @Autowired
    public GithubApiClient(@Value("${github.api.base-url:https://api.github.com}") final String baseUrl) {
        this(buildClient(baseUrl));
    }

    GithubApiClient(final RestClient restClient) {
        this.restClient = restClient;
    }

    private static RestClient buildClient(final String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", API_VERSION)
                .requestInterceptor((request, body, execution) -> {
                    log.debug("GitHub API request: {}", request.getURI());
                    return execution.execute(request, body);
                })
                .build();
    }

    public GithubUser getAuthenticatedUser(final String accessToken) {
        return restClient.get()
                .uri("/user")
                .headers(withAuth(accessToken))
                .retrieve()
                .onStatus(HttpStatus.UNAUTHORIZED::equals, (request, res) -> {
                    throw new GithubTokenInvalidException("GitHub rejected the access token");
                })
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (request, res) -> {
                    log.debug("GitHub API returned {} for GET /user", res.getStatusCode());
                    throw new GithubApiException("GitHub API request failed");
                })
                .body(GithubUser.class);
    }

    public GithubRepo getRepository(final String accessToken, final String owner, final String repo) {
        return restClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .headers(withAuth(accessToken))
                .retrieve()
                .onStatus(HttpStatus.UNAUTHORIZED::equals, (request, res) -> {
                    throw new GithubTokenInvalidException("GitHub rejected the access token");
                })
                .onStatus(HttpStatus.NOT_FOUND::equals, (request, res) -> {
                    throw new GithubRepoNotFoundException(owner, repo);
                })
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (request, res) -> {
                    log.debug("GitHub API returned {} for GET /repos/{}/{}", res.getStatusCode(), owner, repo);
                    throw new GithubApiException("GitHub API request failed");
                })
                .body(GithubRepo.class);
    }

    public Map<String, Long> getRepositoryLanguages(final String accessToken, final String owner, final String repo) {
        var languages = restClient.get()
                .uri("/repos/{owner}/{repo}/languages", owner, repo)
                .headers(withAuth(accessToken))
                .retrieve()
                .onStatus(HttpStatus.UNAUTHORIZED::equals, (request, res) -> {
                    throw new GithubTokenInvalidException("GitHub rejected the access token");
                })
                .onStatus(HttpStatus.NOT_FOUND::equals, (request, res) -> {
                    throw new GithubRepoNotFoundException(owner, repo);
                })
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (request, res) -> {
                    log.debug("GitHub API returned {} for GET /repos/{}/{}/languages", res.getStatusCode(), owner, repo);
                    throw new GithubApiException("GitHub API request failed");
                })
                .body(new ParameterizedTypeReference<Map<String, Long>>() {});
        return languages == null ? Map.of() : languages;
    }

    private Consumer<HttpHeaders> withAuth(final String accessToken) {
        return headers -> {
            if (accessToken != null && !accessToken.isBlank()) {
                headers.setBearerAuth(accessToken);
            }
        };
    }

    public record GithubUser(
            @JsonProperty("id") long id,
            @JsonProperty("login") String login,
            @JsonProperty("name") String name,
            @JsonProperty("avatar_url") String avatarUrl) {}

    public record GithubRepo(
            @JsonProperty("id") long id,
            @JsonProperty("name") String name,
            @JsonProperty("full_name") String fullName,
            @JsonProperty("description") String description,
            @JsonProperty("html_url") String htmlUrl,
            @JsonProperty("private") boolean isPrivate,
            @JsonProperty("stargazers_count") int stargazersCount,
            @JsonProperty("forks_count") int forksCount,
            @JsonProperty("permissions") Permissions permissions) {

        /** {@code permissions} is only present on authenticated responses; absence (an
         * unauthenticated call) is treated as no write access. */
        public boolean hasWriteAccess() {
            return permissions != null && (permissions.push() || permissions.admin());
        }

        public record Permissions(
                @JsonProperty("admin") boolean admin,
                @JsonProperty("push") boolean push,
                @JsonProperty("pull") boolean pull) {}
    }
}
