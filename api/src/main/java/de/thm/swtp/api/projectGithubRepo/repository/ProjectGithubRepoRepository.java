package de.thm.swtp.api.projectGithubRepo.repository;

import de.thm.swtp.api.projectGithubRepo.entity.ProjectGithubRepoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectGithubRepoRepository extends JpaRepository<ProjectGithubRepoEntity, UUID> {

    Optional<ProjectGithubRepoEntity> findByProjectId(UUID projectId);

    void deleteByProjectId(UUID projectId);
}
