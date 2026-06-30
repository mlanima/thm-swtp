package de.thm.swtp.api.tag.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.thm.swtp.api.common.LogSafe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
public class GitHubTopicsClient {

    private static final String SEARCH_URL = "/search/topics";

    private final RestClient restClient;

    public GitHubTopicsClient(
            @Value("${github.topics.api.base-url:https://api.github.com}") final String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.mercy-preview+json")
                .requestInterceptor((request, body, execution) -> {
                    log.debug("GitHub Topics API request: {}", request.getURI());
                    return execution.execute(request, body);
                })
                .build();
    }

    public boolean tagExists(final String tagName) {
        var response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(SEARCH_URL)
                        .queryParam("q", tagName)
                        .build())
                .retrieve()
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
