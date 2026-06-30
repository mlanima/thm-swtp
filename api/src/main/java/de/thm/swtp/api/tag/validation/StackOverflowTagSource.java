package de.thm.swtp.api.tag.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.thm.swtp.api.common.LogSafe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(name = "app.tags.source", havingValue = "stackoverflow")
public class StackOverflowTagSource implements TagSource {

    private static final String TAG_INFO_URL = "/tags/{tags}/info";
    private static final int QUOTA_WARN_THRESHOLD = 100;

    private final RestClient.Builder restClientBuilder;

    @Value("${stackoverflow.api.base-url:https://api.stackexchange.com/2.3}")
    private String baseUrl;

    @Value("${stackoverflow.api.key:}")
    private String apiKeyValue;

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
                .requestInterceptor((request, body, execution) -> {
                    log.debug("StackOverflow API request: {} {}", request.getMethod(), request.getURI().getPath());
                    return execution.execute(request, body);
                })
                .build();
    }

    @Override
    public boolean tagExists(final String tagName) {
        var apiKey = apiKeyValue.isBlank() ? Optional.empty() : Optional.of(apiKeyValue);

        var response = restClient.get()
                .uri(uriBuilder -> {
                    var uri = uriBuilder.path(TAG_INFO_URL)
                            .queryParam("site", "stackoverflow");
                    apiKey.ifPresent(key -> uri.queryParam("key", key));
                    return uri.build(tagName);
                })
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        (request, res) -> {
                            log.error("StackOverflow API returned {} for tag: {}",
                                    res.getStatusCode(), LogSafe.clean(tagName));
                            throw new TagValidationException("Tag validation service temporarily unavailable");
                        })
                .body(StackOverflowResponse.class);

        if (response == null) {
            log.error("StackOverflow API returned null response for tag: {}", LogSafe.clean(tagName));
            throw new TagValidationException("StackOverflow API returned empty response");
        }

        if (response.backoff() != null && response.backoff() > 0) {
            log.warn("StackOverflow API requested backoff of {}s for tag: {}",
                    response.backoff(), LogSafe.clean(tagName));
        }

        if (response.quotaRemaining() != null && response.quotaRemaining() < QUOTA_WARN_THRESHOLD) {
            log.warn("StackOverflow API quota nearly exhausted: {}/{} remaining",
                    response.quotaRemaining(), response.quotaMax());
        }

        boolean exists = response.items() != null && !response.items().isEmpty();
        log.debug("Tag '{}' exists on StackOverflow: {}", LogSafe.clean(tagName), exists);
        return exists;
    }

    private record StackOverflowResponse(
            @JsonProperty("items") List<StackOverflowItem> items,
            @JsonProperty("backoff") Integer backoff,
            @JsonProperty("quota_max") Integer quotaMax,
            @JsonProperty("quota_remaining") Integer quotaRemaining) {}

    private record StackOverflowItem(
            @JsonProperty("name") String name,
            @JsonProperty("count") int count) {}
}
