package de.thm.swtp.api.projectGithubRepo.service;

import de.thm.swtp.api.github.client.GithubApiClient;
import de.thm.swtp.api.github.exception.GithubConnectionRequiredException;
import de.thm.swtp.api.github.exception.GithubReadmeNotEnabledException;
import de.thm.swtp.api.github.exception.GithubRepoAccessDeniedException;
import de.thm.swtp.api.github.exception.GithubRepoNotLinkedException;
import de.thm.swtp.api.github.service.GithubConnectionService;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import de.thm.swtp.api.projectGithubRepo.domain.GithubRepoData;
import de.thm.swtp.api.projectGithubRepo.entity.ProjectGithubRepoEntity;
import de.thm.swtp.api.projectGithubRepo.repository.ProjectGithubRepoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectGithubRepoServiceTest {

    private static final GithubApiClient.GithubRepo.Permissions WRITE_ACCESS =
            new GithubApiClient.GithubRepo.Permissions(false, true, true);
    private static final GithubApiClient.GithubRepo.Permissions NO_WRITE_ACCESS =
            new GithubApiClient.GithubRepo.Permissions(false, false, true);

    private ProjectGithubRepoRepository projectGithubRepoRepository;
    private ProjectRepository projectRepository;
    private GithubConnectionService githubConnectionService;
    private GithubApiClient githubApiClient;
    private GithubRepoDataService githubRepoDataService;
    private ProjectGithubRepoService service;

    private UUID projectId;
    private UUID userId;
    private ProjectEntity project;

    @BeforeEach
    void setUp() {
        projectGithubRepoRepository = mock(ProjectGithubRepoRepository.class);
        projectRepository = mock(ProjectRepository.class);
        githubConnectionService = mock(GithubConnectionService.class);
        githubApiClient = mock(GithubApiClient.class);
        githubRepoDataService = mock(GithubRepoDataService.class);

        service = new ProjectGithubRepoService(
                projectGithubRepoRepository, projectRepository, githubConnectionService,
                githubApiClient, githubRepoDataService);

        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        project = ProjectEntity.builder().id(projectId).build();
    }

    @Test
    void shouldThrowWhenProjectNotFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.link(projectId, userId, "mlanima", "thm-swtp"))
                .isInstanceOf(ProjectNotFoundException.class);
        verifyNoInteractions(githubConnectionService, githubApiClient);
    }

    @Test
    void shouldThrowWhenNoActiveGithubConnection() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(githubConnectionService.getActiveDecryptedToken(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.link(projectId, userId, "mlanima", "thm-swtp"))
                .isInstanceOf(GithubConnectionRequiredException.class);
        verifyNoInteractions(githubApiClient);
    }

    @Test
    void shouldThrowWhenUserLacksWriteAccess() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(githubConnectionService.getActiveDecryptedToken(userId)).thenReturn(Optional.of("gho_token"));
        when(githubApiClient.getRepository("gho_token", "mlanima", "thm-swtp"))
                .thenReturn(new GithubApiClient.GithubRepo(1L, "thm-swtp", "mlanima/thm-swtp",
                        "desc", "https://github.com/mlanima/thm-swtp", false, 1, 0, "main", NO_WRITE_ACCESS));

        assertThatThrownBy(() -> service.link(projectId, userId, "mlanima", "thm-swtp"))
                .isInstanceOf(GithubRepoAccessDeniedException.class);
        verify(projectGithubRepoRepository, never()).save(any());
    }

    @Test
    void shouldLinkPrivateRepoWhenUserHasWriteAccess() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(githubConnectionService.getActiveDecryptedToken(userId)).thenReturn(Optional.of("gho_token"));
        when(githubApiClient.getRepository("gho_token", "mlanima", "private-repo"))
                .thenReturn(new GithubApiClient.GithubRepo(1L, "private-repo", "mlanima/private-repo",
                        "desc", "https://github.com/mlanima/private-repo", true, 0, 0, "main", WRITE_ACCESS));
        when(projectGithubRepoRepository.save(any(ProjectGithubRepoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(projectGithubRepoRepository.findByProjectId(projectId))
                .thenReturn(Optional.empty())
                .thenAnswer(invocation -> Optional.of(ProjectGithubRepoEntity.builder()
                        .project(project)
                        .repoOwner("mlanima")
                        .repoName("private-repo")
                        .linkedByKeycloakId(userId)
                        .build()));
        when(githubRepoDataService.fetch("mlanima", "private-repo", userId))
                .thenReturn(GithubRepoData.builder().fullName("mlanima/private-repo").languages(List.of()).build());

        var card = service.link(projectId, userId, "mlanima", "private-repo");

        verify(projectGithubRepoRepository).save(any(ProjectGithubRepoEntity.class));
        assertThat(card.getLink().getRepoOwner()).isEqualTo("mlanima");
    }

    @Test
    void shouldLinkPublicRepoAndReturnCard() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(githubConnectionService.getActiveDecryptedToken(userId)).thenReturn(Optional.of("gho_token"));
        when(githubApiClient.getRepository("gho_token", "mlanima", "thm-swtp"))
                .thenReturn(new GithubApiClient.GithubRepo(1L, "thm-swtp", "mlanima/thm-swtp",
                        "desc", "https://github.com/mlanima/thm-swtp", false, 7, 2, "main", WRITE_ACCESS));
        when(projectGithubRepoRepository.save(any(ProjectGithubRepoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var savedEntityCaptor = org.mockito.ArgumentCaptor.forClass(ProjectGithubRepoEntity.class);

        // getCard() is called internally after save; findByProjectId must then return the saved entity.
        when(projectGithubRepoRepository.findByProjectId(projectId))
                .thenReturn(Optional.empty())
                .thenAnswer(invocation -> Optional.of(ProjectGithubRepoEntity.builder()
                        .project(project)
                        .repoOwner("mlanima")
                        .repoName("thm-swtp")
                        .linkedByKeycloakId(userId)
                        .build()));
        when(githubRepoDataService.fetch("mlanima", "thm-swtp", userId))
                .thenReturn(GithubRepoData.builder()
                        .fullName("mlanima/thm-swtp")
                        .htmlUrl("https://github.com/mlanima/thm-swtp")
                        .description("desc")
                        .stargazersCount(7)
                        .forksCount(2)
                        .languages(List.of())
                        .build());

        var card = service.link(projectId, userId, "mlanima", "thm-swtp");

        verify(projectGithubRepoRepository).save(savedEntityCaptor.capture());
        assertThat(savedEntityCaptor.getValue().getRepoOwner()).isEqualTo("mlanima");
        assertThat(savedEntityCaptor.getValue().getRepoName()).isEqualTo("thm-swtp");
        assertThat(savedEntityCaptor.getValue().getLinkedByKeycloakId()).isEqualTo(userId);
        assertThat(card.getData().getFullName()).isEqualTo("mlanima/thm-swtp");
        assertThat(card.isDataUnavailable()).isFalse();
    }

    @Test
    void shouldReplaceExistingLinkOnRelink() {
        var existing = ProjectGithubRepoEntity.builder()
                .project(project)
                .repoOwner("old-owner")
                .repoName("old-repo")
                .linkedByKeycloakId(userId)
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(githubConnectionService.getActiveDecryptedToken(userId)).thenReturn(Optional.of("gho_token"));
        when(githubApiClient.getRepository("gho_token", "new-owner", "new-repo"))
                .thenReturn(new GithubApiClient.GithubRepo(2L, "new-repo", "new-owner/new-repo",
                        "desc", "https://github.com/new-owner/new-repo", false, 0, 0, "main", WRITE_ACCESS));
        when(projectGithubRepoRepository.findByProjectId(projectId)).thenReturn(Optional.of(existing));
        when(projectGithubRepoRepository.save(any(ProjectGithubRepoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(githubRepoDataService.fetch("new-owner", "new-repo", userId))
                .thenReturn(GithubRepoData.builder().fullName("new-owner/new-repo").languages(List.of()).build());

        var card = service.link(projectId, userId, "new-owner", "new-repo");

        assertThat(existing.getRepoOwner()).isEqualTo("new-owner");
        assertThat(existing.getRepoName()).isEqualTo("new-repo");
        assertThat(card.getLink().getRepoOwner()).isEqualTo("new-owner");
    }

    @Test
    void shouldThrowWhenGettingCardForUnlinkedProject() {
        when(projectGithubRepoRepository.findByProjectId(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCard(projectId))
                .isInstanceOf(GithubRepoNotLinkedException.class);
    }

    @Test
    void shouldReturnCardWithDataUnavailableWhenFetchReturnsNull() {
        var entity = ProjectGithubRepoEntity.builder()
                .project(project)
                .repoOwner("mlanima")
                .repoName("thm-swtp")
                .linkedByKeycloakId(userId)
                .build();
        when(projectGithubRepoRepository.findByProjectId(projectId)).thenReturn(Optional.of(entity));
        when(githubRepoDataService.fetch("mlanima", "thm-swtp", userId)).thenReturn(null);

        var card = service.getCard(projectId);

        assertThat(card.isDataUnavailable()).isTrue();
        assertThat(card.getData()).isNull();
    }

    @Test
    void shouldDeleteLinkOnUnlink() {
        var entity = ProjectGithubRepoEntity.builder().project(project).build();
        when(projectGithubRepoRepository.findByProjectId(projectId)).thenReturn(Optional.of(entity));

        service.unlink(projectId);

        verify(projectGithubRepoRepository).delete(entity);
    }

    @Test
    void shouldThrowWhenUnlinkingUnlinkedProject() {
        when(projectGithubRepoRepository.findByProjectId(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unlink(projectId))
                .isInstanceOf(GithubRepoNotLinkedException.class);
        verify(projectGithubRepoRepository, never()).delete(any(ProjectGithubRepoEntity.class));
    }

    @Test
    void shouldEnableReadmeVisibility() {
        var entity = ProjectGithubRepoEntity.builder()
                .project(project)
                .repoOwner("mlanima")
                .repoName("thm-swtp")
                .linkedByKeycloakId(userId)
                .build();
        when(projectGithubRepoRepository.findByProjectId(projectId)).thenReturn(Optional.of(entity));
        when(githubRepoDataService.fetch("mlanima", "thm-swtp", userId))
                .thenReturn(GithubRepoData.builder().fullName("mlanima/thm-swtp").languages(List.of()).build());

        service.setReadmeVisibility(projectId, true);

        assertThat(entity.isShowReadme()).isTrue();
        verify(projectGithubRepoRepository).save(entity);
    }

    @Test
    void shouldThrowWhenSettingReadmeVisibilityForUnlinkedProject() {
        when(projectGithubRepoRepository.findByProjectId(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setReadmeVisibility(projectId, true))
                .isInstanceOf(GithubRepoNotLinkedException.class);
    }

    @Test
    void shouldReturnReadmeWhenEnabledAndAvailable() {
        var entity = ProjectGithubRepoEntity.builder()
                .project(project)
                .repoOwner("mlanima")
                .repoName("thm-swtp")
                .linkedByKeycloakId(userId)
                .defaultBranch("main")
                .showReadme(true)
                .build();
        when(projectGithubRepoRepository.findByProjectId(projectId)).thenReturn(Optional.of(entity));
        when(githubRepoDataService.fetchReadme("mlanima", "thm-swtp", userId)).thenReturn("# Hello");

        var readme = service.getReadme(projectId);

        assertThat(readme.getMarkdown()).isEqualTo("# Hello");
        assertThat(readme.getDefaultBranch()).isEqualTo("main");
        assertThat(readme.isAvailable()).isTrue();
    }

    @Test
    void shouldReturnUnavailableReadmeWhenFetchFails() {
        var entity = ProjectGithubRepoEntity.builder()
                .project(project)
                .repoOwner("mlanima")
                .repoName("thm-swtp")
                .linkedByKeycloakId(userId)
                .showReadme(true)
                .build();
        when(projectGithubRepoRepository.findByProjectId(projectId)).thenReturn(Optional.of(entity));
        when(githubRepoDataService.fetchReadme("mlanima", "thm-swtp", userId)).thenReturn(null);

        var readme = service.getReadme(projectId);

        assertThat(readme.isAvailable()).isFalse();
        assertThat(readme.getMarkdown()).isNull();
    }

    @Test
    void shouldThrowWhenReadmeNotEnabled() {
        var entity = ProjectGithubRepoEntity.builder()
                .project(project)
                .repoOwner("mlanima")
                .repoName("thm-swtp")
                .linkedByKeycloakId(userId)
                .showReadme(false)
                .build();
        when(projectGithubRepoRepository.findByProjectId(projectId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.getReadme(projectId))
                .isInstanceOf(GithubReadmeNotEnabledException.class);
        verifyNoInteractions(githubRepoDataService);
    }

    @Test
    void shouldThrowWhenGettingReadmeForUnlinkedProject() {
        when(projectGithubRepoRepository.findByProjectId(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReadme(projectId))
                .isInstanceOf(GithubRepoNotLinkedException.class);
    }
}
