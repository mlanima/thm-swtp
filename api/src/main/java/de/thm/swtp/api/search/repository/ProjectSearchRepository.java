package de.thm.swtp.api.search.repository;

import de.thm.swtp.api.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectSearchRepository extends JpaRepository<Project, UUID> {

    // TODO: also filter by isPrivateProject = false to exclude private projects from search results
    List<Project> findByNameContainingIgnoreCase(String name);
}
