package de.thm.swtp.api.projectFavorite.repository;

import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.projectFavorite.entity.ProjectFavoriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectFavoriteRepository extends JpaRepository<ProjectFavoriteEntity, UUID> {

    boolean existsByUserKeycloakIdAndProjectId(UUID userKeycloakId, UUID projectId);

    long countByProjectId(UUID projectId);

    Optional<ProjectFavoriteEntity> findByUserKeycloakIdAndProjectId(UUID userKeycloakId, UUID projectId);

    @Query("SELECT DISTINCT f.project FROM ProjectFavoriteEntity f " +
           "LEFT JOIN FETCH f.project.owner " +
           "LEFT JOIN FETCH f.project.members " +
           "WHERE f.user.keycloakId = :userId AND f.project.deletedAt IS NULL")
    List<ProjectEntity> findProjectsByUserKeycloakId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM ProjectFavoriteEntity f WHERE f.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") UUID projectId);
}
