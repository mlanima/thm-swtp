package de.thm.swtp.api.project;

import de.thm.swtp.api.auditlog.domain.AuditActor;
import de.thm.swtp.api.auditlog.service.AuditLogService;
import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import de.thm.swtp.api.discord.repository.DiscordChannelSettingsRepository;
import de.thm.swtp.api.discord.repository.DiscordMessageSyncRepository;
import de.thm.swtp.api.links.repository.ProjectLinkRepository;
import de.thm.swtp.api.moderation.ContentModerationService;
import de.thm.swtp.api.moderation.exception.ContentNotValidException;
import de.thm.swtp.api.project.dto.request.CreateProjectRequest;
import de.thm.swtp.api.project.dto.request.UpdateProjectRequest;
import de.thm.swtp.api.project.dto.response.DeleteProjectResponse;
import de.thm.swtp.api.project.dto.response.ProjectResponse;
import de.thm.swtp.api.project.exception.ExceptionInvalidProjectUrl;
import de.thm.swtp.api.project.exception.ExceptionProjectNameAlreadyExists;
import de.thm.swtp.api.project.exception.ExceptionProjectNotFound;
import de.thm.swtp.api.project.exception.ExceptionProjectResponse;
import de.thm.swtp.api.projectFiles.service.ProjectFileService;
import de.thm.swtp.api.projectGithubRepo.repository.ProjectGithubRepoRepository;
import de.thm.swtp.api.projectInvitation.repository.ProjectInviteRepository;
import de.thm.swtp.api.projectJoinRequest.repository.ProjectJoinRequestRepository;
import de.thm.swtp.api.projectPost.entity.ProjectPostEntity;
import de.thm.swtp.api.projectPost.repository.ProjectPostRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import de.thm.swtp.api.discord.repository.LinkedChannelRepository;
import de.thm.swtp.api.discord.service.DiscordNotificationService;
import de.thm.swtp.api.discord.service.DiscordProjectService;
import de.thm.swtp.api.projectFavorite.repository.ProjectFavoriteRepository;
import de.thm.swtp.api.projectView.repository.ProjectViewRepository;
import de.thm.swtp.api.projectView.entity.ProjectViewEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

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
    private LinkedChannelRepository linkedChannelRepository;

    @Mock
    private DiscordNotificationService discordNotificationService;

    @Mock
    private ContentModerationService contentModerationService;

    @Mock
    private ProjectGithubRepoRepository projectGithubRepoRepository;

    @Mock
    private ProjectInviteRepository projectInviteRepository;

    @Mock
    private DiscordProjectService discordProjectService;
    private ProjectJoinRequestRepository projectJoinRequestRepository;

    @Mock
    private ProjectPostRepository  projectPostRepository;

    @Mock
    private DiscordMessageSyncRepository discordMessageSyncRepository;

    @Mock
    private ProjectFileService projectFileService;

    @Mock
    private DiscordChannelSettingsRepository discordChannelSettingsRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ProjectLinkRepository projectLinkRepository;

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

        UUID viewerId = UUID.randomUUID();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectFavoriteRepository.countByProjectId(projectId)).thenReturn(12L);
        when(projectViewRepository.countByProjectId(projectId)).thenReturn(247L);
        when(userProfileRepository.findById(viewerId)).thenReturn(Optional.empty());

        ProjectResponse response = projectService.getProject(projectId, viewerId);

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

        UUID viewerId = UUID.randomUUID();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectFavoriteRepository.countByProjectId(projectId)).thenReturn(0L);
        when(projectViewRepository.countByProjectId(projectId)).thenReturn(6L);
        when(userProfileRepository.findById(viewerId)).thenReturn(Optional.empty());

        ProjectResponse response = projectService.getProject(projectId, viewerId);

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

        UUID viewerId = UUID.randomUUID();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectFavoriteRepository.countByProjectId(projectId)).thenReturn(0L);
        when(projectViewRepository.countByProjectId(projectId)).thenReturn(0L);
        when(userProfileRepository.findById(viewerId)).thenReturn(Optional.empty());

        ProjectResponse response = projectService.getProject(projectId, viewerId);

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

    @Test
    void deleteProject_shouldDeleteDependenciesInForeignKeySafeOrder() {
        UUID projectId = UUID.randomUUID();
        UUID postId1 = UUID.randomUUID();
        UUID postId2 = UUID.randomUUID();
        UUID linkedChannelId = UUID.randomUUID();

        AuditActor actor = new AuditActor(
                UUID.randomUUID(),
                "moderator",
                "moderator@mod.de"
        );

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("Project to delete")
                .projectUrl("project-to-delete")
                .build();

        ProjectPostEntity firstPost = ProjectPostEntity.builder()
                .id(postId1)
                .project(project)
                .build();

        ProjectPostEntity secondPost = ProjectPostEntity.builder()
                .id(postId2)
                .project(project)
                .build();

        LinkedChannelEntity linkedChannel = LinkedChannelEntity.builder()
                .id(linkedChannelId)
                .project(project)
                .discordChannelId("123456789")
                .build();

        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(project));

        when(projectPostRepository.findAllByProjectId(projectId))
                .thenReturn(List.of(firstPost, secondPost));

        when(linkedChannelRepository.findByProjectId(projectId))
                .thenReturn(Optional.of(linkedChannel));

        DeleteProjectResponse response =
                projectService.deleteProject(projectId, actor);

        assertThat(response.getProjectId()).isEqualTo(projectId);

        InOrder order = inOrder(
                projectPostRepository,
                discordMessageSyncRepository,
                projectFileService,
                linkedChannelRepository,
                discordChannelSettingsRepository,
                projectGithubRepoRepository,
                projectLinkRepository,
                projectFavoriteRepository,
                projectViewRepository,
                projectInviteRepository,
                projectJoinRequestRepository,
                projectRepository,
                auditLogService
        );

        // Retrieve posts, then delete the synced posts
        order.verify(projectPostRepository)
                .findAllByProjectId(projectId);

        order.verify(discordMessageSyncRepository)
                .deleteByPlatformPostId(postId1);

        order.verify(discordMessageSyncRepository)
                .deleteByPlatformPostId(postId2);

        // Delete posts.
        order.verify(projectPostRepository)
                .deleteByProjectId(projectId);

        order.verify(projectFileService)
                .deleteAllProjectFiles(projectId);

        order.verify(linkedChannelRepository)
                .findByProjectId(projectId);

        order.verify(discordChannelSettingsRepository)
                .deleteByLinkedChannelId(linkedChannelId);

        order.verify(linkedChannelRepository)
                .deleteByProjectId(projectId);

        order.verify(projectGithubRepoRepository)
                .deleteByProjectId(projectId);

        order.verify(projectLinkRepository)
                .deleteByProjectId(projectId);

        order.verify(projectFavoriteRepository)
                .deleteByProjectId(projectId);

        order.verify(projectViewRepository)
                .deleteByProjectId(projectId);

        order.verify(projectInviteRepository)
                .deleteByProjectId(projectId);

        order.verify(projectJoinRequestRepository)
                .deleteByProjectId(projectId);

        order.verify(projectRepository)
                .delete(project);

        order.verify(auditLogService)
                .logProjectDeleted(actor, projectId, project.getName());

        order.verify(projectRepository)
                .flush();
    }
}