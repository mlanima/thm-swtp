package de.thm.swtp.api.links.github;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubRepoRefTest {

    @ParameterizedTest
    @CsvSource({
            "https://github.com/octocat/Hello-World, octocat, Hello-World",
            "https://github.com/octocat/Hello-World/, octocat, Hello-World",
            "https://github.com/octocat/Hello-World.git, octocat, Hello-World",
            "https://github.com/octocat/Hello-World/tree/main, octocat, Hello-World",
            "http://github.com/octocat/Hello-World, octocat, Hello-World",
            "https://www.github.com/octocat/Hello-World, octocat, Hello-World",
            "https://GITHUB.com/octocat/Hello-World, octocat, Hello-World",
            "https://github.com/my-org/my.repo, my-org, my.repo",
    })
    void parse_shouldExtractOwnerAndRepo_forValidGitHubRepoUrls(String url, String expectedOwner, String expectedRepo) {
        Optional<GitHubRepoRef> result = GitHubRepoRef.parse(url);

        assertThat(result).isPresent();
        assertThat(result.get().owner()).isEqualTo(expectedOwner);
        assertThat(result.get().repo()).isEqualTo(expectedRepo);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://gitlab.com/octocat/Hello-World",
            "https://github.com/octocat",
            "https://github.com",
            "not-a-url",
            "https://github.com/",
    })
    void parse_shouldReturnEmpty_forNonRepoOrNonGitHubUrls(String url) {
        assertThat(GitHubRepoRef.parse(url)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {""})
    void parse_shouldReturnEmpty_forBlankUrl(String url) {
        assertThat(GitHubRepoRef.parse(url)).isEmpty();
    }
}
