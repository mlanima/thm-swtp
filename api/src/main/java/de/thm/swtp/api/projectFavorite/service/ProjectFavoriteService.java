package de.thm.swtp.api.projectFavorite.service;

import de.thm.swtp.api.common.TxLogger;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ExceptionProjectAlreadyDeleted;
import de.thm.swtp.api.project.exception.ExceptionProjectNotFound;
import de.thm.swtp.api.projectFavorite.entity.ProjectFavoriteEntity;
import de.thm.swtp.api.projectFavorite.exception.ProjectAlreadyFavoritedException;
import de.thm.swtp.api.projectFavorite.exception.ProjectFavoriteNotFoundException;
import de.thm.swtp.api.projectFavorite.repository.ProjectFavoriteRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectFavoriteService {

    private final ProjectFavoriteRepository projectFavoriteRepository;
    private final ProjectRepository projectRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public void addFavorite(UUID projectId, UUID userId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ExceptionProjectNotFound(projectId));

        if (project.getDeletedAt() != null) {
            throw new ExceptionProjectAlreadyDeleted(projectId);
        }

        if (projectFavoriteRepository.existsByUserKeycloakIdAndProjectId(userId, projectId)) {
            throw new ProjectAlreadyFavoritedException(projectId);
        }

        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId.toString()));

        projectFavoriteRepository.save(
                ProjectFavoriteEntity.builder()
                        .user(user)
                        .project(project)
                        .build()
        );
        TxLogger.afterCommit(log, "Favorite added: project={}, user={}", projectId, userId);
    }

    @Transactional
    public void removeFavorite(UUID projectId, UUID userId) {
        ProjectFavoriteEntity favorite = projectFavoriteRepository
                .findByUserKeycloakIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ProjectFavoriteNotFoundException(projectId));

        ProjectEntity project = favorite.getProject();
        projectFavoriteRepository.delete(favorite);
        TxLogger.afterCommit(log, "Favorite removed: project={}, user={}", projectId, userId);
    }

    @Transactional(readOnly = true)
    public List<ProjectEntity> getFavorites(UUID userId) {
        return projectFavoriteRepository.findProjectsByUserKeycloakId(userId);
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(UUID projectId, UUID userId) {
        return projectFavoriteRepository.existsByUserKeycloakIdAndProjectId(userId, projectId);
    }

    @Transactional(readOnly = true)
    public long countFavorites(UUID projectId) {
        return projectFavoriteRepository.countByProjectId(projectId);
    }
}
