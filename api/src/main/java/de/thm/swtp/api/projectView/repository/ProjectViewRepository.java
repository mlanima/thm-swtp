package de.thm.swtp.api.projectView.repository;

import de.thm.swtp.api.projectView.entity.ProjectViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectViewRepository extends JpaRepository<ProjectViewEntity, UUID> {

    long countByProjectId(UUID projectId);
    void deleteByProjectId(UUID projectId);
}