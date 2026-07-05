package de.thm.swtp.api.links.github;

import de.thm.swtp.api.common.LogSafe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Slf4j
@Component
public class GitHubReadmeClient {

    private static final String README_URL = "/repos/{owner}/{repo}/readme";

    private final RestClient restClient;

    public GitHubReadmeClient(
            @Value("${github.readme.api.base-url:https://api.github.com}") final String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.raw+json")
                .requestInterceptor((request, body, execution) -> {
                    log.debug("GitHub Readme API request: {}", request.getURI());
                    return execution.execute(request, body);
                })
                .build();
    }

    public Optional<String> fetchReadme(final String owner, final String repo) {
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(README_URL, owner, repo)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, res) -> { })
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("GitHub Readme API returned {} for {}/{}",
                        response.getStatusCode(), LogSafe.clean(owner), LogSafe.clean(repo));
                return Optional.empty();
            }

            return Optional.of(response.getBody());
        } catch (RestClientException e) {
            log.warn("GitHub Readme API call failed for {}/{}: {}",
                    LogSafe.clean(owner), LogSafe.clean(repo), e.getMessage());
            return Optional.empty();
        }
    }
}
