package de.thm.swtp.api.project;

import de.thm.swtp.api.project.dto.response.ProjectResponse;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private ProjectFavoriteRepository projectFavoriteRepository;

    @Mock
    private ProjectViewRepository projectViewRepository;

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