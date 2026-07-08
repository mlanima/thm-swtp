package de.thm.swtp.api.projectGithubRepo.service;

import de.thm.swtp.api.github.client.GithubApiClient;
import de.thm.swtp.api.github.exception.GithubApiException;
import de.thm.swtp.api.github.exception.GithubRepoNotFoundException;
import de.thm.swtp.api.github.exception.GithubTokenInvalidException;
import de.thm.swtp.api.github.service.GithubConnectionService;
import de.thm.swtp.api.projectGithubRepo.domain.GithubRepoData;
import de.thm.swtp.api.projectGithubRepo.domain.LanguageShare;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/** Fetches (and caches) the public GitHub metadata shown on a project's repo card and README.
 * Uses the linker's token when available for the higher authenticated rate limit (and private-repo
 * access), falling back to an unauthenticated call — since a linked repo the linker no longer has
 * access to just renders as unavailable rather than failing outright. Cache keys are owner/repo
 * only; the token never enters the cache. */
@Slf4j
@Service
@RequiredArgsConstructor
public class GithubRepoDataService {

    private static final int MAX_LANGUAGES = 6;

    private final GithubApiClient githubApiClient;
    private final GithubConnectionService githubConnectionService;

    @Cacheable(cacheNames = "github-repo-card", key = "#owner + '/' + #repo")
    public GithubRepoData fetch(String owner, String repo, UUID linkedByKeycloakId) {
        return fetchWithFallback(linkedByKeycloakId, owner, repo, token -> {
            var repository = githubApiClient.getRepository(token, owner, repo);
            var languages = githubApiClient.getRepositoryLanguages(token, owner, repo);
            return buildData(repository, languages);
        });
    }

    @Cacheable(cacheNames = "github-readme", key = "#owner + '/' + #repo")
    public String fetchReadme(String owner, String repo, UUID linkedByKeycloakId) {
        return fetchWithFallback(linkedByKeycloakId, owner, repo,
                token -> githubApiClient.getReadme(token, owner, repo));
    }

    /** Tries the linker's active token first; on 401 marks the connection invalid and falls back
     * to an unauthenticated call; on not-found/API error returns {@code null} so callers can
     * render an "unavailable" state instead of failing the request outright. */
    private <T> T fetchWithFallback(UUID linkerId, String owner, String repo, Function<String, T> fetcher) {
        var token = githubConnectionService.getActiveDecryptedToken(linkerId);
        if (token.isPresent()) {
            try {
                return fetcher.apply(token.get());
            } catch (GithubTokenInvalidException e) {
                githubConnectionService.markInvalid(linkerId);
                log.warn("GitHub token for user {} rejected while fetching {}/{}; falling back to unauthenticated",
                        linkerId, owner, repo);
            } catch (GithubApiException | GithubRepoNotFoundException e) {
                log.debug("GitHub data unavailable for {}/{}: {}", owner, repo, e.getMessage());
                return null;
            }
        }

        try {
            return fetcher.apply(null);
        } catch (GithubApiException | GithubRepoNotFoundException | GithubTokenInvalidException e) {
            log.debug("GitHub data unavailable for {}/{}: {}", owner, repo, e.getMessage());
            return null;
        }
    }

    private GithubRepoData buildData(GithubApiClient.GithubRepo repo, Map<String, Long> languages) {
        long total = languages.values().stream().mapToLong(Long::longValue).sum();

        var topLanguages = languages.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(MAX_LANGUAGES)
                .map(entry -> LanguageShare.builder()
                        .name(entry.getKey())
                        .percentage(total == 0 ? 0.0 : (entry.getValue() * 100.0) / total)
                        .build())
                .toList();

        return GithubRepoData.builder()
                .fullName(repo.fullName())
                .htmlUrl(repo.htmlUrl())
                .description(repo.description())
                .stargazersCount(repo.stargazersCount())
                .forksCount(repo.forksCount())
                .languages(topLanguages)
                .build();
    }
}
