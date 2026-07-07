package de.thm.swtp.api.projectView.repository;

import de.thm.swtp.api.projectView.entity.ProjectViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProjectViewRepository extends JpaRepository<ProjectViewEntity, UUID> {

    long countByProjectId(UUID projectId);
    void deleteByProjectId(UUID projectId);

    @Query("SELECT v.project.id AS projectId, MAX(v.viewedAt) AS lastViewedAt FROM ProjectViewEntity v "
            + "WHERE v.user.keycloakId = :userId AND v.project.id IN :projectIds GROUP BY v.project.id")
    List<ProjectLastViewed> findLastViewedByUserAndProjectIdIn(
            @Param("userId") UUID userId,
            @Param("projectIds") Collection<UUID> projectIds);

    interface ProjectLastViewed {
        UUID getProjectId();
        LocalDateTime getLastViewedAt();
    }
}
