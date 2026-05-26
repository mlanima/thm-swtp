package de.thm.swtp.api.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {
    boolean existsByName(String name);
}
