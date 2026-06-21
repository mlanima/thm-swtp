package de.thm.swtp.api.projectFiles.repository;

import de.thm.swtp.api.projectFiles.entity.ProjectFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectFileRepository extends JpaRepository<ProjectFileEntity, UUID> {

    List<ProjectFileEntity> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

    long countByProjectId(UUID projectId);
}
