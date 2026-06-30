package de.thm.swtp.api.tag.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

class GitHubTopicsClientTest {

    private RestClient restClient;
    private MockRestServiceServer mockServer;
    private GitHubTopicsClient client;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .build();
        mockServer = MockRestServiceServer.bindTo(restClient).build();
        client = new GitHubTopicsClient(restClient);
    }

    @Test
    void tagExists_shouldReturnTrue_whenExactMatchFound() {
        mockServer.expect(requestTo("https://api.github.com/search/topics?q=java"))
                .andRespond(withSuccess(
                        "{\"total_count\":1,\"items\":[{\"name\":\"java\"}]}",
                        MediaType.APPLICATION_JSON));

        boolean result = client.tagExists("java");

        assertThat(result).isTrue();
        mockServer.verify();
    }

    @Test
    void tagExists_shouldReturnTrue_whenExactMatchDiffersInCase() {
        mockServer.expect(requestTo("https://api.github.com/search/topics?q=Java"))
                .andRespond(withSuccess(
                        "{\"total_count\":1,\"items\":[{\"name\":\"java\"}]}",
                        MediaType.APPLICATION_JSON));

        boolean result = client.tagExists("Java");

        assertThat(result).isTrue();
        mockServer.verify();
    }

    @Test
    void tagExists_shouldReturnFalse_whenNoExactMatchButFulltextResultsExist() {
        mockServer.expect(requestTo("https://api.github.com/search/topics?q=javas"))
                .andRespond(withSuccess(
                        "{\"total_count\":3,\"items\":[{\"name\":\"java\"},{\"name\":\"javascript\"},{\"name\":\"javafx\"}]}",
                        MediaType.APPLICATION_JSON));

        boolean result = client.tagExists("javas");

        assertThat(result).isFalse();
        mockServer.verify();
    }

    @Test
    void tagExists_shouldReturnFalse_whenNoResults() {
        mockServer.expect(requestTo("https://api.github.com/search/topics?q=nonexistent12345"))
                .andRespond(withSuccess(
                        "{\"total_count\":0,\"items\":[]}",
                        MediaType.APPLICATION_JSON));

        boolean result = client.tagExists("nonexistent12345");

        assertThat(result).isFalse();
        mockServer.verify();
    }

    @Test
    void tagExists_shouldThrowTagValidationException_on4xx() {
        mockServer.expect(requestTo("https://api.github.com/search/topics?q=java"))
                .andRespond(withStatus(FORBIDDEN));

        assertThatThrownBy(() -> client.tagExists("java"))
                .isInstanceOf(TagValidationException.class)
                .hasMessage("Tag validation service temporarily unavailable");
        mockServer.verify();
    }

    @Test
    void tagExists_shouldThrowTagValidationException_on5xx() {
        mockServer.expect(requestTo("https://api.github.com/search/topics?q=java"))
                .andRespond(withStatus(INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.tagExists("java"))
                .isInstanceOf(TagValidationException.class)
                .hasMessage("Tag validation service temporarily unavailable");
        mockServer.verify();
    }

    @Test
    void tagExists_shouldThrowTagValidationException_onNullResponse() {
        mockServer.expect(requestTo("https://api.github.com/search/topics?q=java"))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.tagExists("java"))
                .isInstanceOf(TagValidationException.class)
                .hasMessage("Tag validation service temporarily unavailable");
        mockServer.verify();
    }
}
