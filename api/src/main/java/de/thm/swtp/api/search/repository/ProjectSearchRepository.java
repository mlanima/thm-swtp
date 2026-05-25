package de.thm.swtp.api.search.repository;

import de.thm.swtp.api.project.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectSearchRepository extends JpaRepository<ProjectEntity, UUID> {

    List<ProjectEntity> findByNameContainingIgnoreCase(String name);
}
