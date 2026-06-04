package de.thm.swtp.api.project;

import de.thm.swtp.api.project.dto.response.ProjectResponse;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
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
                .members(List.of(member))
                .viewsCount(247)
                .likesCount(12)
                .openPositionsCount(3)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        ProjectResponse response = projectService.getProject(projectId);

        assertThat(response.getId()).isEqualTo(projectId);
        assertThat(response.getStats()).isNotNull();
        assertThat(response.getStats().getContributors()).isEqualTo(2);
        assertThat(response.getStats().getViews()).isEqualTo(248);
        assertThat(response.getStats().getLikes()).isEqualTo(12);
        assertThat(response.getStats().getOpenPositions()).isEqualTo(3);

        verify(projectRepository).save(project);
    }

    @Test
    void shouldIncreaseViewsWhenProjectIsLoaded() {
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
                .viewsCount(5)
                .likesCount(0)
                .openPositionsCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        ProjectResponse response = projectService.getProject(projectId);

        assertThat(response.getStats().getViews()).isEqualTo(6);

        ArgumentCaptor<ProjectEntity> projectCaptor = ArgumentCaptor.forClass(ProjectEntity.class);
        verify(projectRepository).save(projectCaptor.capture());

        ProjectEntity savedProject = projectCaptor.getValue();

        assertThat(savedProject.getViewsCount()).isEqualTo(6);
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
                .viewsCount(0)
                .likesCount(0)
                .openPositionsCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        ProjectResponse response = projectService.getProject(projectId);

        assertThat(response.getStats().getContributors()).isEqualTo(1);
    }
}