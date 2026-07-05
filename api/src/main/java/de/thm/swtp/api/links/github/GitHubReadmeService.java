package de.thm.swtp.api.links.github;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class GitHubReadmeService {

    private final GitHubReadmeClient gitHubReadmeClient;

    @Cacheable(value = "github-readme", key = "#owner.toLowerCase() + '/' + #repo.toLowerCase()")
    public Optional<String> getReadme(final String owner, final String repo) {
        return gitHubReadmeClient.fetchReadme(owner, repo);
    }
}
