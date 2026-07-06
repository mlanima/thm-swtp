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

/** Fetches (and caches) the public GitHub metadata shown on a project's repo card. Uses the
 * linker's token when available for the higher authenticated rate limit, falling back to an
 * unauthenticated call — since only public repos can be linked, that fallback always works
 * unless the repo was deleted or renamed. The cache key is owner/repo only; the token never
 * enters the cache. */
@Slf4j
@Service
@RequiredArgsConstructor
public class GithubRepoDataService {

    private static final int MAX_LANGUAGES = 6;

    private final GithubApiClient githubApiClient;
    private final GithubConnectionService githubConnectionService;

    @Cacheable(cacheNames = "github-repo-card", key = "#owner + '/' + #repo")
    public GithubRepoData fetch(String owner, String repo, UUID linkedByKeycloakId) {
        var token = githubConnectionService.getActiveDecryptedToken(linkedByKeycloakId);
        if (token.isPresent()) {
            try {
                return fetchWithToken(owner, repo, token.get());
            } catch (GithubTokenInvalidException e) {
                githubConnectionService.markInvalid(linkedByKeycloakId);
                log.warn("GitHub token for user {} rejected while fetching {}/{}; falling back to unauthenticated",
                        linkedByKeycloakId, owner, repo);
            } catch (GithubApiException | GithubRepoNotFoundException e) {
                log.debug("GitHub repo data unavailable for {}/{}: {}", owner, repo, e.getMessage());
                return null;
            }
        }

        try {
            return fetchWithToken(owner, repo, null);
        } catch (GithubApiException | GithubRepoNotFoundException | GithubTokenInvalidException e) {
            log.debug("GitHub repo data unavailable for {}/{}: {}", owner, repo, e.getMessage());
            return null;
        }
    }

    private GithubRepoData fetchWithToken(String owner, String repo, String token) {
        var repository = githubApiClient.getRepository(token, owner, repo);
        var languages = githubApiClient.getRepositoryLanguages(token, owner, repo);
        return buildData(repository, languages);
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
