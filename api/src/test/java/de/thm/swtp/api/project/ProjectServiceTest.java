package de.thm.swtp.api.project;

import de.thm.swtp.api.moderation.ContentModerationService;
import de.thm.swtp.api.moderation.exception.ContentNotValidException;
import de.thm.swtp.api.project.dto.request.CreateProjectRequest;
import de.thm.swtp.api.project.dto.request.UpdateProjectRequest;
import de.thm.swtp.api.project.dto.response.ProjectResponse;
import de.thm.swtp.api.project.exception.ExceptionInvalidProjectUrl;
import de.thm.swtp.api.project.exception.ExceptionProjectNameAlreadyExists;
import de.thm.swtp.api.project.exception.ExceptionProjectResponse;
import de.thm.swtp.api.projectGithubRepo.repository.ProjectGithubRepoRepository;
import de.thm.swtp.api.projectInvitation.repository.ProjectInviteRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import de.thm.swtp.api.projectFavorite.repository.ProjectFavoriteRepository;
import de.thm.swtp.api.projectView.repository.ProjectViewRepository;
import de.thm.swtp.api.projectView.entity.ProjectViewEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    private static final UUID OWNER_ID = UUID.randomUUID();

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private ProjectFavoriteRepository projectFavoriteRepository;

    @Mock
    private ProjectViewRepository projectViewRepository;

    @Mock
    private ContentModerationService contentModerationService;

    @Mock
    private ProjectGithubRepoRepository projectGithubRepoRepository;

    @Mock
    private ProjectInviteRepository projectInviteRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void shouldReturnProjectWithStatsAndIncreaseViews() {
        UserProfile owner = UserProfile.builder()
                .keycloakId(UUID.randomUUID())
                .username("owner")
                .email("owner@mni.thm.de")
                .build();

        UserProfile member = UserProfile.builder()
                .keycloakId(UUID.randomUUID())
                .username("member")
                .email("member@mni.thm.de")
                .build();

        UUID projectId = UUID.randomUUID();

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("Testprojekt")
                .description("Beschreibung")
                .projectUrl("testprojekt")
                .isPrivateProject(false)
                .owner(owner)
                .members(Set.of(member))
                .openPositionsCount(3)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectFavoriteRepository.countByProjectId(projectId)).thenReturn(12L);
        when(projectViewRepository.countByProjectId(projectId)).thenReturn(247L);

        ProjectResponse response = projectService.getProject(projectId);

        assertThat(response.getId()).isEqualTo(projectId);
        assertThat(response.getStats()).isNotNull();
        assertThat(response.getStats().getContributors()).isEqualTo(2);
        assertThat(response.getStats().getViews()).isEqualTo(247);
        assertThat(response.getStats().getLikes()).isEqualTo(12);
        assertThat(response.getStats().getOpenPositions()).isEqualTo(3);

        verify(projectViewRepository).save(any(ProjectViewEntity.class));
        verify(projectViewRepository).countByProjectId(projectId);
        verify(projectRepository, never()).save(project);
    }

    @Test
    void shouldAddViewWhenProjectIsLoaded() {
        UserProfile owner = UserProfile.builder()
                .keycloakId(UUID.randomUUID())
                .username("owner")
                .email("owner@mni.thm.de")
                .build();

        UUID projectId = UUID.randomUUID();

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("Testprojekt")
                .description("Beschreibung")
                .projectUrl("testprojekt")
                .isPrivateProject(false)
                .owner(owner)
                .openPositionsCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectFavoriteRepository.countByProjectId(projectId)).thenReturn(0L);
        when(projectViewRepository.countByProjectId(projectId)).thenReturn(6L);

        ProjectResponse response = projectService.getProject(projectId);

        assertThat(response.getStats().getViews()).isEqualTo(6);

        verify(projectViewRepository).save(any(ProjectViewEntity.class));
        verify(projectViewRepository).countByProjectId(projectId);
        verify(projectRepository, never()).save(project);
    }

    @Test
    void shouldReturnOneContributorWhenOnlyOwnerExists() {
        UserProfile owner = UserProfile.builder()
                .keycloakId(UUID.randomUUID())
                .username("owner")
                .email("owner@mni.thm.de")
                .build();

        UUID projectId = UUID.randomUUID();

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("Testprojekt")
                .description("Beschreibung")
                .projectUrl("testprojekt")
                .isPrivateProject(false)
                .owner(owner)
                .openPositionsCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectFavoriteRepository.countByProjectId(projectId)).thenReturn(0L);
        when(projectViewRepository.countByProjectId(projectId)).thenReturn(0L);

        ProjectResponse response = projectService.getProject(projectId);

        assertThat(response.getStats().getContributors()).isEqualTo(1);
    }

    // ── createProject — moderation ──────────────────────────────────────────────

    @Test
    void createProject_shouldModerateAllTextFields() {
        UserProfile owner = UserProfile.builder()
                .keycloakId(OWNER_ID)
                .username("owner")
                .email("owner@mni.thm.de")
                .build();
        when(projectRepository.existsByName("My Project")).thenReturn(false);
        when(userProfileRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(projectRepository.existsByProjectUrl(anyString())).thenReturn(false);
        when(projectRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        projectService.createProject(new CreateProjectRequest(
                "My Project", "A description", "Short desc", null, false, null, null
        ), OWNER_ID);

        verify(contentModerationService).assertAppropriate("My Project", "name");
        verify(contentModerationService).assertAppropriate("A description", "description");
        verify(contentModerationService).assertAppropriate("Short desc", "shortDescription");
        verify(projectRepository).save(any());
    }

    @Test
    void createProject_shouldCheckNameConflictBeforeModeration() {
        when(projectRepository.existsByName("Existing")).thenReturn(true);

        assertThatThrownBy(() -> projectService.createProject(new CreateProjectRequest(
                "Existing", "desc", "short", null, false, null, null
        ), OWNER_ID))
                .isInstanceOf(ExceptionProjectResponse.class);

        verify(contentModerationService, never()).assertAppropriate(any(), any());
        verify(projectRepository, never()).save(any());
    }

    @Test
    void createProject_shouldThrow_whenNameFlagged() {
        when(projectRepository.existsByName("bad")).thenReturn(false);
        doThrow(new ContentNotValidException("name"))
                .when(contentModerationService).assertAppropriate("bad", "name");

        assertThatThrownBy(() -> projectService.createProject(new CreateProjectRequest(
                "bad", "desc", "short", null, false, null, null
        ), OWNER_ID))
                .isInstanceOf(ContentNotValidException.class);

        verify(projectRepository, never()).save(any());
    }

    @Test
    void createProject_shouldThrow_whenDescriptionFlagged() {
        when(projectRepository.existsByName("Project")).thenReturn(false);
        lenient().doThrow(new ContentNotValidException("description"))
                .when(contentModerationService).assertAppropriate("bad desc", "description");

        assertThatThrownBy(() -> projectService.createProject(new CreateProjectRequest(
                "Project", "bad desc", "short", null, false, null, null
        ), OWNER_ID))
                .isInstanceOf(ContentNotValidException.class);

        verify(projectRepository, never()).save(any());
    }

    @Test
    void createProject_shouldThrow_whenShortDescriptionFlagged() {
        when(projectRepository.existsByName("Project")).thenReturn(false);
        lenient().doThrow(new ContentNotValidException("shortDescription"))
                .when(contentModerationService).assertAppropriate("bad short", "shortDescription");

        assertThatThrownBy(() -> projectService.createProject(new CreateProjectRequest(
                "Project", "desc", "bad short", null, false, null, null
        ), OWNER_ID))
                .isInstanceOf(ContentNotValidException.class);

        verify(projectRepository, never()).save(any());
    }

    @Test
    void createProject_shouldSucceed_whenAllContentAppropriate() {
        UserProfile owner = UserProfile.builder()
                .keycloakId(OWNER_ID)
                .username("owner")
                .email("owner@mni.thm.de")
                .build();
        when(projectRepository.existsByName("Good Project")).thenReturn(false);
        when(userProfileRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(projectRepository.existsByProjectUrl(anyString())).thenReturn(false);
        var captured = ArgumentCaptor.forClass(ProjectEntity.class);
        when(projectRepository.save(captured.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.createProject(new CreateProjectRequest(
                "Good Project", "Good description", "Good short", "good-url", false, null, null
        ), OWNER_ID);

        assertThat(response.getName()).isEqualTo("Good Project");
        assertThat(response.getDescription()).isEqualTo("Good description");
        assertThat(response.getShortDescription()).isEqualTo("Good short");
        assertThat(response.getProjectUrl()).isEqualTo("good-url");
        verify(contentModerationService).assertAppropriate("Good Project", "name");
        verify(contentModerationService).assertAppropriate("Good description", "description");
        verify(contentModerationService).assertAppropriate("Good short", "shortDescription");
        verify(contentModerationService).assertAppropriate("good-url", "projectUrl");
    }

    // ── editProject — moderation ────────────────────────────────────────────────

    @Test
    void editProject_shouldModerateName() {
        UUID projectId = UUID.randomUUID();
        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("Original")
                .description("Desc")
                .projectUrl("orig-url")
                .isPrivateProject(false)
                .owner(UserProfile.builder().keycloakId(OWNER_ID).build())
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new UpdateProjectRequest();
        request.setName("New Name");

        projectService.editProject(projectId, request);

        verify(contentModerationService).assertAppropriate("New Name", "name");
    }

    @Test
    void editProject_shouldModerateDescription() {
        UUID projectId = UUID.randomUUID();
        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("Original")
                .description("Desc")
                .projectUrl("orig-url")
                .isPrivateProject(false)
                .owner(UserProfile.builder().keycloakId(OWNER_ID).build())
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new UpdateProjectRequest();
        request.setDescription("New Desc");

        projectService.editProject(projectId, request);

        verify(contentModerationService).assertAppropriate("New Desc", "description");
    }

    @Test
    void editProject_shouldModerateShortDescription() {
        UUID projectId = UUID.randomUUID();
        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("Original")
                .description("Desc")
                .projectUrl("orig-url")
                .isPrivateProject(false)
                .owner(UserProfile.builder().keycloakId(OWNER_ID).build())
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new UpdateProjectRequest();
        request.setShortDescription("New Short");

        projectService.editProject(projectId, request);

        verify(contentModerationService).assertAppropriate("New Short", "shortDescription");
    }

    @Test
    void editProject_shouldModerateProjectUrl() {
        UUID projectId = UUID.randomUUID();
        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("Original")
                .description("Desc")
                .projectUrl("orig-url")
                .isPrivateProject(false)
                .owner(UserProfile.builder().keycloakId(OWNER_ID).build())
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.existsByProjectUrl("new-url")).thenReturn(false);
        when(projectRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new UpdateProjectRequest();
        request.setProjectUrl("new-url");

        projectService.editProject(projectId, request);

        verify(contentModerationService).assertAppropriate("new-url", "projectUrl");
    }

    @Test
    void editProject_shouldModerateName_whenNameUnchanged() {
        UUID projectId = UUID.randomUUID();
        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("Same Name")
                .description("Desc")
                .projectUrl("orig-url")
                .isPrivateProject(false)
                .owner(UserProfile.builder().keycloakId(OWNER_ID).build())
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new UpdateProjectRequest();
        request.setName("Same Name");

        projectService.editProject(projectId, request);

        verify(contentModerationService).assertAppropriate("Same Name", "name");
        verify(projectRepository).save(any());
    }

    @Test
    void editProject_shouldCheckNameConflictBeforeModeration() {
        UUID projectId = UUID.randomUUID();
        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("Original")
                .description("Desc")
                .projectUrl("orig-url")
                .isPrivateProject(false)
                .owner(UserProfile.builder().keycloakId(OWNER_ID).build())
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.existsByNameAndIdNot("Existing", projectId)).thenReturn(true);

        var request = new UpdateProjectRequest();
        request.setName("Existing");

        assertThatThrownBy(() -> projectService.editProject(projectId, request))
                .isInstanceOf(ExceptionProjectNameAlreadyExists.class);

        verify(contentModerationService, never()).assertAppropriate(any(), any());
        verify(projectRepository, never()).save(any());
    }

    @Test
    void editProject_shouldThrow_whenNameFlagged() {
        UUID projectId = UUID.randomUUID();
        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("Original")
                .description("Desc")
                .projectUrl("orig-url")
                .isPrivateProject(false)
                .owner(UserProfile.builder().keycloakId(OWNER_ID).build())
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        doThrow(new ContentNotValidException("name"))
                .when(contentModerationService).assertAppropriate("bad", "name");

        var request = new UpdateProjectRequest();
        request.setName("bad");

        assertThatThrownBy(() -> projectService.editProject(projectId, request))
                .isInstanceOf(ContentNotValidException.class);

        verify(projectRepository, never()).save(any());
    }

    @Test
    void editProject_shouldThrow_whenProjectUrlInvalidFormat() {
        UUID projectId = UUID.randomUUID();
        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("Original")
                .description("Desc")
                .projectUrl("orig-url")
                .isPrivateProject(false)
                .owner(UserProfile.builder().keycloakId(OWNER_ID).build())
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        var request = new UpdateProjectRequest();
        request.setProjectUrl("INVALID URL!");

        assertThatThrownBy(() -> projectService.editProject(projectId, request))
                .isInstanceOf(ExceptionInvalidProjectUrl.class);

        verify(contentModerationService, never()).assertAppropriate(any(), any());
        verify(projectRepository, never()).save(any());
    }

    @Test
    void transferProjectOwnership_shouldUnlinkGithubRepo() {
        UserProfile owner = UserProfile.builder()
                .keycloakId(UUID.randomUUID())
                .username("owner")
                .email("owner@mni.thm.de")
                .build();

        UserProfile newOwner = UserProfile.builder()
                .keycloakId(UUID.randomUUID())
                .username("member")
                .email("member@mni.thm.de")
                .build();

        UUID projectId = UUID.randomUUID();

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("Testprojekt")
                .description("Beschreibung")
                .projectUrl("testprojekt")
                .isPrivateProject(false)
                .owner(owner)
                .members(new HashSet<>(Set.of(newOwner)))
                .openPositionsCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        projectService.transferProjectOwnership(projectId, newOwner.getKeycloakId());

        assertThat(project.getOwner()).isEqualTo(newOwner);
        verify(projectGithubRepoRepository).deleteByProjectId(projectId);
    }
}