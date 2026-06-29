package de.thm.swtp.api.tag.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.thm.swtp.api.common.LogSafe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.tags.source", havingValue = "stackoverflow", matchIfMissing = true)
public class StackOverflowTagSource implements TagSource {

    private static final String TAG_INFO_URL = "/2.3/tags/{tags}/info?site=stackoverflow";
    private static final int QUOTA_WARN_THRESHOLD = 100;

    private final RestClient restClient;
    private final Optional<String> apiKey;

    public StackOverflowTagSource(
            @Value("${stackoverflow.api.base-url:https://api.stackexchange.com/2.3}") final String baseUrl,
            @Value("${stackoverflow.api.key:}") final Optional<String> apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor((request, body, execution) -> {
                    log.debug("StackOverflow API request: {}", request.getURI());
                    return execution.execute(request, body);
                })
                .build();
    }

    @Override
    public boolean tagExists(final String tagName) {
        var response = restClient.get()
                .uri(uriBuilder -> {
                    var uri = uriBuilder.path(TAG_INFO_URL);
                    apiKey.ifPresent(key -> uri.queryParam("key", key));
                    return uri.build(tagName);
                })
                .retrieve()
                .body(StackOverflowResponse.class);

        if (response == null) {
            log.warn("StackOverflow API returned null response for tag: {}", LogSafe.clean(tagName));
            throw new TagValidationException("StackOverflow API returned empty response");
        }

        if (response.backoff() > 0) {
            log.warn("StackOverflow API requested backoff of {}s for tag: {}",
                    response.backoff(), LogSafe.clean(tagName));
        }

        if (response.quotaRemaining() < QUOTA_WARN_THRESHOLD) {
            log.warn("StackOverflow API quota nearly exhausted: {}/{} remaining",
                    response.quotaRemaining(), response.quotaMax());
        }

        boolean exists = response.items() != null && !response.items().isEmpty();
        log.debug("Tag '{}' exists on StackOverflow: {}", LogSafe.clean(tagName), exists);
        return exists;
    }

    private record StackOverflowResponse(
            @JsonProperty("items") List<StackOverflowItem> items,
            @JsonProperty("backoff") int backoff,
            @JsonProperty("quota_max") int quotaMax,
            @JsonProperty("quota_remaining") int quotaRemaining) {}

    private record StackOverflowItem(
            @JsonProperty("name") String name,
            @JsonProperty("count") int count) {}
}
