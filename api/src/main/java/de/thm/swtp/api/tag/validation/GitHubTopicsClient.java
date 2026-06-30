package de.thm.swtp.api.tag.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.thm.swtp.api.common.LogSafe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Slf4j
@RequiredArgsConstructor(access = lombok.AccessLevel.PACKAGE)
@Component
public class GitHubTopicsClient {

    private static final String SEARCH_URL = "/search/topics";

    private final RestClient.Builder restClientBuilder;

    @Value("${github.topics.api.base-url:https://api.github.com}")
    private String baseUrl;

    private RestClient restClient;

    @PostConstruct
    void init() {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.mercy-preview+json")
                .requestInterceptor((request, body, execution) -> {
                    log.debug("GitHub Topics API request: {}", request.getURI());
                    return execution.execute(request, body);
                })
                .build();
    }

    GitHubTopicsClient(final RestClient restClient) {
        this.restClient = restClient;
        this.restClientBuilder = null;
    }

    public boolean tagExists(final String tagName) {
        var response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(SEARCH_URL)
                        .queryParam("q", tagName)
                        .build())
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        (request, res) -> {
                            log.error("GitHub Topics API returned {} for tag: {}",
                                    res.getStatusCode(), LogSafe.clean(tagName));
                            throw new TagValidationException("Tag validation service temporarily unavailable");
                        })
                .body(GitHubSearchResponse.class);

        if (response == null) {
            log.error("GitHub Topics API returned null response for tag: {}", LogSafe.clean(tagName));
            throw new TagValidationException("Tag validation service temporarily unavailable");
        }

        var exactMatch = response.items() != null
                && response.items().stream()
                        .anyMatch(item -> item.name().equalsIgnoreCase(tagName));
        log.debug("Tag '{}' exists on GitHub Topics: {}", LogSafe.clean(tagName), exactMatch);
        return exactMatch;
    }

    record GitHubSearchResponse(
            @JsonProperty("total_count") int totalCount,
            @JsonProperty("items") List<GitHubTopicItem> items) {}

    record GitHubTopicItem(
            @JsonProperty("name") String name) {}
}
