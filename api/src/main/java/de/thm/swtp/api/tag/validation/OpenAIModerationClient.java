package de.thm.swtp.api.tag.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.thm.swtp.api.common.LogSafe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class OpenAIModerationClient {

    private static final String MODERATION_URL = "/v1/moderations";

    private final RestClient restClient;
    private final String model;
    private final double threshold;
    private final boolean enabled;

    @Autowired
    public OpenAIModerationClient(
            @Value("${openai.api.base-url:https://api.openai.com}") final String baseUrl,
            @Value("${openai.moderation.model:omni-moderation-latest}") final String model,
            @Value("${openai.moderation.threshold:0.1}") final double threshold,
            @Value("${openai.api.key:}") final String apiKey) {
        this(buildClient(baseUrl, apiKey), model, threshold, !apiKey.isBlank());
    }

    OpenAIModerationClient(
            final RestClient restClient,
            final String model,
            final double threshold,
            final boolean enabled) {
        this.restClient = restClient;
        this.model = model;
        this.threshold = threshold;
        this.enabled = enabled;
    }

    private static RestClient buildClient(final String baseUrl, final String apiKey) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor((request, body, execution) -> {
                    log.debug("OpenAI Moderation API request: {}", request.getURI());
                    return execution.execute(request, body);
                })
                .build();
    }

    public boolean isFlagged(final String text) {
        if (!enabled) {
            log.warn("OpenAI moderation disabled \u2014 no API key configured");
            throw new TagValidationException("Tag validation service temporarily unavailable");
        }

        var response = restClient.post()
                .uri(MODERATION_URL)
                .body(new ModerationRequest(model, text))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        (request, res) -> {
                            log.debug("OpenAI Moderation API returned {} for input: {}",
                                    res.getStatusCode(), LogSafe.clean(text));
                            throw new TagValidationException("Tag validation service temporarily unavailable");
                        })
                .body(ModerationResponse.class);

        if (response == null || response.results() == null || response.results().isEmpty()) {
            log.debug("OpenAI Moderation API returned empty response for input: {}", LogSafe.clean(text));
            throw new TagValidationException("Tag validation service temporarily unavailable");
        }

        var result = response.results().getFirst();
        var scores = Optional.ofNullable(result.categoryScores()).orElse(Map.of());

        var anyScoreAboveThreshold = scores.values().stream()
                .anyMatch(score -> score >= threshold);

        if (result.flagged() || anyScoreAboveThreshold) {
            log.debug("Input flagged by OpenAI moderation (flagged={}, scores={}): {}",
                    result.flagged(), scores, LogSafe.clean(text));
            return true;
        }

        log.debug("Input passed OpenAI moderation: {}", LogSafe.clean(text));
        return false;
    }

    private record ModerationRequest(
            @JsonProperty("model") String model,
            @JsonProperty("input") String input) {}

    private record ModerationResponse(
            @JsonProperty("id") String id,
            @JsonProperty("model") String model,
            @JsonProperty("results") List<ModerationResult> results) {}

    private record ModerationResult(
            @JsonProperty("flagged") boolean flagged,
            @JsonProperty("categories") Map<String, Boolean> categories,
            @JsonProperty("category_scores") Map<String, Double> categoryScores) {}
}
