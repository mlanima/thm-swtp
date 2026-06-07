package de.thm.swtp.api.projectFavorite.service;

import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.projectFavorite.entity.ProjectFavoriteEntity;
import de.thm.swtp.api.projectFavorite.exception.ProjectAlreadyFavoritedException;
import de.thm.swtp.api.projectFavorite.exception.ProjectFavoriteNotFoundException;
import de.thm.swtp.api.projectFavorite.repository.ProjectFavoriteRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectFavoriteServiceTest {

    @Mock
    private ProjectFavoriteRepository projectFavoriteRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private ProjectFavoriteService projectFavoriteService;

    @Test
    void shouldAddFavorite() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UserProfile user = UserProfile.builder()
                .keycloakId(userId)
                .username("testuser")
                .email("test@mni.thm.de")
                .build();

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("Testproject")
                .projectUrl("testproject")
                .owner(user)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectFavoriteRepository.existsByUserKeycloakIdAndProjectId(userId, projectId)).thenReturn(false);
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(user));

        projectFavoriteService.addFavorite(projectId, userId);

        verify(projectFavoriteRepository).save(any(ProjectFavoriteEntity.class));
        verify(projectRepository, never()).save(project);
    }

    @Test
    void shouldThrowExceptionWhenProjectIsAlreadyFavorited() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UserProfile owner = UserProfile.builder()
                .keycloakId(UUID.randomUUID())
                .username("owner")
                .email("owner@mni.thm.de")
                .build();

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("Testproject")
                .projectUrl("testproject")
                .owner(owner)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectFavoriteRepository.existsByUserKeycloakIdAndProjectId(userId, projectId)).thenReturn(true);

        assertThatThrownBy(() -> projectFavoriteService.addFavorite(projectId, userId))
                .isInstanceOf(ProjectAlreadyFavoritedException.class);

        verify(projectFavoriteRepository, never()).save(any(ProjectFavoriteEntity.class));
        verify(projectRepository, never()).save(project);
    }

    @Test
    void shouldRemoveFavorite() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UserProfile user = UserProfile.builder()
                .keycloakId(userId)
                .username("testuser")
                .email("test@mni.thm.de")
                .build();

        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("Testproject")
                .projectUrl("testproject")
                .owner(user)
                .build();

        ProjectFavoriteEntity favorite = ProjectFavoriteEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .project(project)
                .build();

        when(projectFavoriteRepository.findByUserKeycloakIdAndProjectId(userId, projectId))
                .thenReturn(Optional.of(favorite));

        projectFavoriteService.removeFavorite(projectId, userId);

        verify(projectFavoriteRepository).delete(favorite);
        verify(projectRepository, never()).save(project);
    }

    @Test
    void shouldThrowExceptionWhenFavoriteToRemoveDoesNotExist() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(projectFavoriteRepository.findByUserKeycloakIdAndProjectId(userId, projectId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectFavoriteService.removeFavorite(projectId, userId))
                .isInstanceOf(ProjectFavoriteNotFoundException.class);

        verify(projectFavoriteRepository, never()).delete(any(ProjectFavoriteEntity.class));
        verify(projectRepository, never()).save(any(ProjectEntity.class));
    }
}