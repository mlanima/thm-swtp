package de.thm.swtp.api.tag.validation;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAIModerationClientTest {

    private OpenAIModerationClient clientWithResponse(final int statusCode, final String body) {
        var interceptor = (ClientHttpRequestInterceptor) (request, bodyBytes, execution) -> {
            var response = new MockClientHttpResponse(
                    body.getBytes(StandardCharsets.UTF_8),
                    HttpStatus.valueOf(statusCode));
            response.getHeaders().set("Content-Type", "application/json");
            return response;
        };
        var restClient = RestClient.builder()
                .baseUrl("https://test.openai.com")
                .requestInterceptor(interceptor)
                .build();
        return new OpenAIModerationClient(restClient, "omni-moderation-latest", 0.1, true);
    }

    @Test
    void shouldReturnTrueWhenFlagged() {
        var json = """
                {"results": [{"flagged": true, "categories": {"hate": true}, "category_scores": {"hate": 0.95}}]}
                """;
        assertThat(clientWithResponse(200, json).isFlagged("kill")).isTrue();
    }

    @Test
    void shouldReturnTrueWhenScoreAboveThreshold() {
        var json = """
                {"results": [{"flagged": false, "categories": {}, "category_scores": {"harassment": 0.85}}]}
                """;
        assertThat(clientWithResponse(200, json).isFlagged("some harassing text")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenClean() {
        var json = """
                {"results": [{"flagged": false, "categories": {}, "category_scores": {}}]}
                """;
        assertThat(clientWithResponse(200, json).isFlagged("hello world")).isFalse();
    }

    @Test
    void shouldReturnFalseWhenAllScoresBelowThreshold() {
        var json = """
                {"results": [{"flagged": false, "categories": {}, "category_scores": {"hate": 0.03, "harassment": 0.01}}]}
                """;
        assertThat(clientWithResponse(200, json).isFlagged("typescript")).isFalse();
    }

    @Test
    void shouldThrowWhenServerError() {
        assertThatThrownBy(() -> clientWithResponse(500, "Internal Server Error").isFlagged("test"))
                .isInstanceOf(TagValidationException.class)
                .hasMessage("Tag validation service temporarily unavailable");
    }

    @Test
    void shouldThrowWhenClientError() {
        assertThatThrownBy(() -> clientWithResponse(401, "Unauthorized").isFlagged("test"))
                .isInstanceOf(TagValidationException.class)
                .hasMessage("Tag validation service temporarily unavailable");
    }

    @Test
    void shouldThrowWhenNullResponse() {
        assertThatThrownBy(() -> clientWithResponse(200, "null").isFlagged("test"))
                .isInstanceOf(TagValidationException.class)
                .hasMessage("Tag validation service temporarily unavailable");
    }

    @Test
    void shouldThrowWhenResultsEmpty() {
        var json = """
                {"results": []}
                """;
        assertThatThrownBy(() -> clientWithResponse(200, json).isFlagged("test"))
                .isInstanceOf(TagValidationException.class)
                .hasMessage("Tag validation service temporarily unavailable");
    }

    @Test
    void shouldThrowWhenApiKeyMissing() {
        var restClient = RestClient.builder().baseUrl("https://test.openai.com").build();
        var clientNoKey = new OpenAIModerationClient(restClient, "omni-moderation-latest", 0.1, false);
        assertThatThrownBy(() -> clientNoKey.isFlagged("test"))
                .isInstanceOf(TagValidationException.class)
                .hasMessage("Tag validation service temporarily unavailable");
    }
}
