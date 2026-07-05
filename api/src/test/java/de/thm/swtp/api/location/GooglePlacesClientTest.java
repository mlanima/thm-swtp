package de.thm.swtp.api.location;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.thm.swtp.api.location.exception.GooglePlacesApiException;
import de.thm.swtp.api.location.exception.InvalidPlaceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GooglePlacesClientTest {

    private GooglePlacesClient clientWithResponse(final int statusCode, final String body) {
        var interceptor = (ClientHttpRequestInterceptor) (request, bodyBytes, execution) -> {
            var response = new MockClientHttpResponse(
                    body.getBytes(StandardCharsets.UTF_8),
                    HttpStatus.valueOf(statusCode));
            response.getHeaders().set("Content-Type", "application/json");
            return response;
        };
        var restClient = RestClient.builder()
                .baseUrl("https://maps.googleapis.com/maps/api/place")
                .requestInterceptor(interceptor)
                .build();
        return new GooglePlacesClient("test-key", new ObjectMapper(), restClient);
    }

    @Test
    void shouldReturnLocationWhenValid() {
        var json = """
                {
                  "status": "OK",
                  "result": {
                    "address_components": [
                      {"long_name": "Munich", "types": ["locality"]},
                      {"long_name": "Germany", "types": ["country"]}
                    ],
                    "formatted_address": "Munich, Germany"
                  }
                }
                """;
        assertThat(clientWithResponse(200, json).validatePlaceId("ChIJ..."))
                .isEqualTo("Munich, Germany");
    }

    @Test
    void shouldUseCityWhenLocalityAbsent() {
        var json = """
                {
                  "status": "OK",
                  "result": {
                    "address_components": [
                      {"long_name": "Munich", "types": ["postal_town"]},
                      {"long_name": "Germany", "types": ["country"]}
                    ],
                    "formatted_address": "Munich, Germany"
                  }
                }
                """;
        assertThat(clientWithResponse(200, json).validatePlaceId("ChIJ..."))
                .isEqualTo("Munich, Germany");
    }

    @Test
    void shouldUseFormattedAddressWhenNoCity() {
        var json = """
                {
                  "status": "OK",
                  "result": {
                    "address_components": [],
                    "formatted_address": "Some Place, Earth"
                  }
                }
                """;
        assertThat(clientWithResponse(200, json).validatePlaceId("ChIJ..."))
                .isEqualTo("Some Place, Earth");
    }

    @Test
    void shouldReturnLocationWithoutCountryWhenNoCountryComponent() {
        var json = """
                {
                  "status": "OK",
                  "result": {
                    "address_components": [
                      {"long_name": "Berlin", "types": ["locality"]}
                    ],
                    "formatted_address": "Berlin, Germany"
                  }
                }
                """;
        assertThat(clientWithResponse(200, json).validatePlaceId("ChIJ..."))
                .isEqualTo("Berlin");
    }

    @Test
    void shouldThrowGooglePlacesApiExceptionOnServerError() {
        assertThatThrownBy(() -> clientWithResponse(500, "Server Error").validatePlaceId("ChIJ..."))
                .isInstanceOf(GooglePlacesApiException.class);
    }

    @Test
    void shouldThrowGooglePlacesApiExceptionOnClientError() {
        assertThatThrownBy(() -> clientWithResponse(403, "Forbidden").validatePlaceId("ChIJ..."))
                .isInstanceOf(GooglePlacesApiException.class);
    }

    @Test
    void shouldThrowInvalidPlaceExceptionWhenStatusNotOk() {
        var json = """
                {"status": "ZERO_RESULTS", "result": {}}
                """;
        assertThatThrownBy(() -> clientWithResponse(200, json).validatePlaceId("ChIJ..."))
                .isInstanceOf(InvalidPlaceException.class);
    }

    @Test
    void shouldThrowGooglePlacesApiExceptionOnOverQueryLimit() {
        var json = """
                {"status": "OVER_QUERY_LIMIT", "error_message": "Quota exceeded"}
                """;
        assertThatThrownBy(() -> clientWithResponse(200, json).validatePlaceId("ChIJ..."))
                .isInstanceOf(GooglePlacesApiException.class);
    }

    @Test
    void shouldThrowGooglePlacesApiExceptionOnRequestDenied() {
        var json = """
                {"status": "REQUEST_DENIED", "error_message": "Key invalid"}
                """;
        assertThatThrownBy(() -> clientWithResponse(200, json).validatePlaceId("ChIJ..."))
                .isInstanceOf(GooglePlacesApiException.class);
    }

    @Test
    void shouldThrowGooglePlacesApiExceptionOnUnknownError() {
        var json = """
                {"status": "UNKNOWN_ERROR", "error_message": "Something went wrong"}
                """;
        assertThatThrownBy(() -> clientWithResponse(200, json).validatePlaceId("ChIJ..."))
                .isInstanceOf(GooglePlacesApiException.class);
    }

    @Test
    void shouldThrowGooglePlacesApiExceptionOnEmptyResponse() {
        assertThatThrownBy(() -> clientWithResponse(200, "").validatePlaceId("ChIJ..."))
                .isInstanceOf(GooglePlacesApiException.class);
    }

    @Test
    void shouldThrowGooglePlacesApiExceptionOnMalformedJson() {
        assertThatThrownBy(() -> clientWithResponse(200, "not json").validatePlaceId("ChIJ..."))
                .isInstanceOf(GooglePlacesApiException.class);
    }
}
