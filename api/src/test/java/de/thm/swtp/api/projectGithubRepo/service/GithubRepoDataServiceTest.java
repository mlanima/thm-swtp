package de.thm.swtp.api.projectGithubRepo.service;

import de.thm.swtp.api.github.client.GithubApiClient;
import de.thm.swtp.api.github.exception.GithubRepoNotFoundException;
import de.thm.swtp.api.github.exception.GithubTokenInvalidException;
import de.thm.swtp.api.github.service.GithubConnectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GithubRepoDataServiceTest {

    private GithubApiClient githubApiClient;
    private GithubConnectionService githubConnectionService;
    private GithubRepoDataService service;
    private UUID linkerId;

    @BeforeEach
    void setUp() {
        githubApiClient = mock(GithubApiClient.class);
        githubConnectionService = mock(GithubConnectionService.class);
        service = new GithubRepoDataService(githubApiClient, githubConnectionService);
        linkerId = UUID.randomUUID();
    }

    @Test
    void shouldFetchWithLinkerTokenWhenActive() {
        when(githubConnectionService.getActiveDecryptedToken(linkerId)).thenReturn(Optional.of("gho_token"));
        when(githubApiClient.getRepository("gho_token", "mlanima", "thm-swtp"))
                .thenReturn(new GithubApiClient.GithubRepo(1L, "thm-swtp", "mlanima/thm-swtp",
                        "desc", "https://github.com/mlanima/thm-swtp", false, 5, 1, "main", null));
        when(githubApiClient.getRepositoryLanguages("gho_token", "mlanima", "thm-swtp"))
                .thenReturn(Map.of("Java", 80L, "TypeScript", 20L));

        var data = service.fetch("mlanima", "thm-swtp", linkerId);

        assertThat(data.getFullName()).isEqualTo("mlanima/thm-swtp");
        assertThat(data.getStargazersCount()).isEqualTo(5);
        assertThat(data.getLanguages()).hasSize(2);
        assertThat(data.getLanguages().getFirst().getName()).isEqualTo("Java");
        assertThat(data.getLanguages().getFirst().getPercentage()).isEqualTo(80.0);
    }

    @Test
    void shouldFallBackToUnauthenticatedWhenNoActiveConnection() {
        when(githubConnectionService.getActiveDecryptedToken(linkerId)).thenReturn(Optional.empty());
        when(githubApiClient.getRepository(null, "mlanima", "thm-swtp"))
                .thenReturn(new GithubApiClient.GithubRepo(1L, "thm-swtp", "mlanima/thm-swtp",
                        "desc", "https://github.com/mlanima/thm-swtp", false, 5, 1, "main", null));
        when(githubApiClient.getRepositoryLanguages(null, "mlanima", "thm-swtp")).thenReturn(Map.of());

        var data = service.fetch("mlanima", "thm-swtp", linkerId);

        assertThat(data).isNotNull();
        assertThat(data.getFullName()).isEqualTo("mlanima/thm-swtp");
    }

    @Test
    void shouldMarkInvalidAndFallBackWhenTokenRejected() {
        when(githubConnectionService.getActiveDecryptedToken(linkerId)).thenReturn(Optional.of("gho_stale"));
        when(githubApiClient.getRepository("gho_stale", "mlanima", "thm-swtp"))
                .thenThrow(new GithubTokenInvalidException("rejected"));
        when(githubApiClient.getRepository(null, "mlanima", "thm-swtp"))
                .thenReturn(new GithubApiClient.GithubRepo(1L, "thm-swtp", "mlanima/thm-swtp",
                        "desc", "https://github.com/mlanima/thm-swtp", false, 5, 1, "main", null));
        when(githubApiClient.getRepositoryLanguages(null, "mlanima", "thm-swtp")).thenReturn(Map.of());

        var data = service.fetch("mlanima", "thm-swtp", linkerId);

        verify(githubConnectionService).markInvalid(linkerId);
        assertThat(data).isNotNull();
    }

    @Test
    void shouldReturnNullWhenRepoNotFound() {
        when(githubConnectionService.getActiveDecryptedToken(linkerId)).thenReturn(Optional.of("gho_token"));
        when(githubApiClient.getRepository("gho_token", "mlanima", "renamed-repo"))
                .thenThrow(new GithubRepoNotFoundException("mlanima", "renamed-repo"));

        var data = service.fetch("mlanima", "renamed-repo", linkerId);

        assertThat(data).isNull();
    }

    @Test
    void shouldReturnNullWhenUnauthenticatedFetchAlsoFails() {
        when(githubConnectionService.getActiveDecryptedToken(linkerId)).thenReturn(Optional.empty());
        when(githubApiClient.getRepository(null, "mlanima", "thm-swtp"))
                .thenThrow(new GithubRepoNotFoundException("mlanima", "thm-swtp"));

        var data = service.fetch("mlanima", "thm-swtp", linkerId);

        assertThat(data).isNull();
    }

    @Test
    void shouldFetchReadmeWithLinkerTokenWhenActive() {
        when(githubConnectionService.getActiveDecryptedToken(linkerId)).thenReturn(Optional.of("gho_token"));
        when(githubApiClient.getReadme("gho_token", "mlanima", "thm-swtp")).thenReturn("# Hello");

        var markdown = service.fetchReadme("mlanima", "thm-swtp", linkerId);

        assertThat(markdown).isEqualTo("# Hello");
    }

    @Test
    void shouldFallBackToUnauthenticatedReadmeWhenNoActiveConnection() {
        when(githubConnectionService.getActiveDecryptedToken(linkerId)).thenReturn(Optional.empty());
        when(githubApiClient.getReadme(null, "mlanima", "thm-swtp")).thenReturn("# Hello");

        var markdown = service.fetchReadme("mlanima", "thm-swtp", linkerId);

        assertThat(markdown).isEqualTo("# Hello");
    }

    @Test
    void shouldMarkInvalidAndFallBackWhenTokenRejectedForReadme() {
        when(githubConnectionService.getActiveDecryptedToken(linkerId)).thenReturn(Optional.of("gho_stale"));
        when(githubApiClient.getReadme("gho_stale", "mlanima", "thm-swtp"))
                .thenThrow(new GithubTokenInvalidException("rejected"));
        when(githubApiClient.getReadme(null, "mlanima", "thm-swtp")).thenReturn("# Hello");

        var markdown = service.fetchReadme("mlanima", "thm-swtp", linkerId);

        verify(githubConnectionService).markInvalid(linkerId);
        assertThat(markdown).isEqualTo("# Hello");
    }

    @Test
    void shouldReturnNullWhenNoReadmeFile() {
        when(githubConnectionService.getActiveDecryptedToken(linkerId)).thenReturn(Optional.of("gho_token"));
        when(githubApiClient.getReadme("gho_token", "mlanima", "thm-swtp"))
                .thenThrow(new GithubRepoNotFoundException("mlanima", "thm-swtp"));

        var markdown = service.fetchReadme("mlanima", "thm-swtp", linkerId);

        assertThat(markdown).isNull();
    }
}
