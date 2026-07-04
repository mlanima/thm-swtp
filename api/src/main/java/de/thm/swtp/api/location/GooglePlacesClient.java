package de.thm.swtp.api.location;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import de.thm.swtp.api.location.exception.GooglePlacesApiException;
import de.thm.swtp.api.location.exception.InvalidPlaceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class GooglePlacesClient {

    private static final String DETAILS_PATH = "/details/json?place_id={placeId}&key={key}&fields=address_components,formatted_address";

    private final RestClient restClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public GooglePlacesClient(
            @Value("${google.api.base-url}") final String baseUrl,
            @Value("${google.api.key}") final String apiKey,
            final ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public String validatePlaceId(final String placeId) {
        var body = restClient.get()
                .uri(DETAILS_PATH, placeId, apiKey)
                .retrieve()
                .body(String.class);

        if (body == null || body.isBlank()) {
            throw new GooglePlacesApiException("Empty response from Google Places API");
        }

        JsonNode node;
        try {
            node = objectMapper.readTree(body);
        } catch (Exception e) {
            throw new GooglePlacesApiException("Failed to parse Google Places response");
        }

        var status = node.path("status").asString();
        if (!"OK".equals(status)) {
            var errorMsg = node.path("error_message").asString("");
            log.warn("Google Places API returned status '{}' for placeId={}: {}", status, placeId, errorMsg);
            throw new InvalidPlaceException("Invalid place: " + placeId);
        }

        var result = node.path("result");
        var addressComponents = result.path("address_components");

        var city = findComponent(addressComponents, "locality", "postal_town");
        var country = findComponent(addressComponents, "country");

        var locationName = city != null ? city : result.path("formatted_address").asString();
        if (country != null) {
            locationName = locationName + ", " + country;
        }

        log.info("Validated placeId={} -> '{}'", placeId, locationName);
        return locationName;
    }

    private static String findComponent(final JsonNode components, final String... targetTypes) {
        return StreamSupport.stream(components.spliterator(), false)
                .filter(c -> StreamSupport.stream(c.path("types").spliterator(), false)
                        .map(JsonNode::asString)
                        .anyMatch(t -> Arrays.asList(targetTypes).contains(t)))
                .map(c -> c.path("long_name").asString())
                .findFirst()
                .orElse(null);
    }
}
