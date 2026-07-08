package de.thm.swtp.api.projectGithubRepo.listener;

import de.thm.swtp.api.github.client.GithubApiClient;
import de.thm.swtp.api.github.domain.GithubConnection;
import de.thm.swtp.api.github.domain.GithubConnectionStatus;
import de.thm.swtp.api.github.exception.GithubApiException;
import de.thm.swtp.api.github.exception.GithubTokenInvalidException;
import de.thm.swtp.api.github.service.GithubConnectionService;
import de.thm.swtp.api.notification.event.ProjectMemberAddedEvent;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.projectGithubRepo.entity.ProjectGithubRepoEntity;
import de.thm.swtp.api.projectGithubRepo.repository.ProjectGithubRepoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GithubCollaboratorInviteListenerTest {

    private ProjectGithubRepoRepository projectGithubRepoRepository;
    private GithubConnectionService githubConnectionService;
    private GithubApiClient githubApiClient;
    private GithubCollaboratorInviteListener listener;

    private UUID projectId;
    private UUID newMemberId;
    private UUID linkerId;
    private ProjectMemberAddedEvent event;

    @BeforeEach
    void setUp() {
        projectGithubRepoRepository = mock(ProjectGithubRepoRepository.class);
        githubConnectionService = mock(GithubConnectionService.class);
        githubApiClient = mock(GithubApiClient.class);
        listener = new GithubCollaboratorInviteListener(
                projectGithubRepoRepository, githubConnectionService, githubApiClient);

        projectId = UUID.randomUUID();
        newMemberId = UUID.randomUUID();
        linkerId = UUID.randomUUID();
        event = new ProjectMemberAddedEvent(projectId, newMemberId);
    }

    private ProjectGithubRepoEntity repoLink(boolean autoInvite) {
        return ProjectGithubRepoEntity.builder()
                .project(ProjectEntity.builder().id(projectId).build())
                .repoOwner("mlanima")
                .repoName("thm-swtp")
                .linkedByKeycloakId(linkerId)
                .autoInviteCollaborators(autoInvite)
                .build();
    }

    private GithubConnection connection() {
        return GithubConnection.builder()
                .keycloakId(newMemberId)
                .githubLogin("octocat")
                .status(GithubConnectionStatus.ACTIVE)
                .build();
    }

    @Test
    void shouldDoNothingWhenNoRepoLinked() {
        when(projectGithubRepoRepository.findByProjectId(projectId)).thenReturn(Optional.empty());

        listener.onProjectMemberAdded(event);

        verifyNoInteractions(githubConnectionService, githubApiClient);
    }

    @Test
    void shouldDoNothingWhenAutoInviteDisabled() {
        when(projectGithubRepoRepository.findByProjectId(projectId)).thenReturn(Optional.of(repoLink(false)));

        listener.onProjectMemberAdded(event);

        verifyNoInteractions(githubConnectionService, githubApiClient);
    }

    @Test
    void shouldDoNothingWhenNewMemberHasNoGithubConnection() {
        when(projectGithubRepoRepository.findByProjectId(projectId)).thenReturn(Optional.of(repoLink(true)));
        when(githubConnectionService.getConnection(newMemberId)).thenReturn(Optional.empty());

        listener.onProjectMemberAdded(event);

        verifyNoInteractions(githubApiClient);
        verify(githubConnectionService, never()).getActiveDecryptedToken(any());
    }

    @Test
    void shouldDoNothingWhenLinkerHasNoActiveToken() {
        when(projectGithubRepoRepository.findByProjectId(projectId)).thenReturn(Optional.of(repoLink(true)));
        when(githubConnectionService.getConnection(newMemberId)).thenReturn(Optional.of(connection()));
        when(githubConnectionService.getActiveDecryptedToken(linkerId)).thenReturn(Optional.empty());

        listener.onProjectMemberAdded(event);

        verifyNoInteractions(githubApiClient);
    }

    @Test
    void shouldInviteCollaboratorOnHappyPath() {
        when(projectGithubRepoRepository.findByProjectId(projectId)).thenReturn(Optional.of(repoLink(true)));
        when(githubConnectionService.getConnection(newMemberId)).thenReturn(Optional.of(connection()));
        when(githubConnectionService.getActiveDecryptedToken(linkerId)).thenReturn(Optional.of("gho_token"));

        listener.onProjectMemberAdded(event);

        verify(githubApiClient).addCollaborator("gho_token", "mlanima", "thm-swtp", "octocat", "push");
    }

    @Test
    void shouldMarkLinkerTokenInvalidAndSwallowExceptionWhenTokenRejected() {
        when(projectGithubRepoRepository.findByProjectId(projectId)).thenReturn(Optional.of(repoLink(true)));
        when(githubConnectionService.getConnection(newMemberId)).thenReturn(Optional.of(connection()));
        when(githubConnectionService.getActiveDecryptedToken(linkerId)).thenReturn(Optional.of("gho_token"));
        doThrow(new GithubTokenInvalidException("rejected"))
                .when(githubApiClient).addCollaborator("gho_token", "mlanima", "thm-swtp", "octocat", "push");

        listener.onProjectMemberAdded(event);

        verify(githubConnectionService).markInvalid(linkerId);
    }

    @Test
    void shouldSwallowGenericGithubApiException() {
        when(projectGithubRepoRepository.findByProjectId(projectId)).thenReturn(Optional.of(repoLink(true)));
        when(githubConnectionService.getConnection(newMemberId)).thenReturn(Optional.of(connection()));
        when(githubConnectionService.getActiveDecryptedToken(linkerId)).thenReturn(Optional.of("gho_token"));
        doThrow(new GithubApiException("boom"))
                .when(githubApiClient).addCollaborator("gho_token", "mlanima", "thm-swtp", "octocat", "push");

        listener.onProjectMemberAdded(event);

        verify(githubConnectionService, never()).markInvalid(any());
    }
}
