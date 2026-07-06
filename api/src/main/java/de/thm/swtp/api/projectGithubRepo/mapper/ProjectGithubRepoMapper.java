package de.thm.swtp.api.projectGithubRepo.mapper;

import de.thm.swtp.api.projectGithubRepo.domain.GithubRepoLink;
import de.thm.swtp.api.projectGithubRepo.entity.ProjectGithubRepoEntity;

public class ProjectGithubRepoMapper {

    public static GithubRepoLink toDomain(ProjectGithubRepoEntity entity) {
        return GithubRepoLink.builder()
                .projectId(entity.getProject().getId())
                .repoOwner(entity.getRepoOwner())
                .repoName(entity.getRepoName())
                .linkedByKeycloakId(entity.getLinkedByKeycloakId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
