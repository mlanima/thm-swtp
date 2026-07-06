package de.thm.swtp.api.projectGithubRepo.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Value
public class GithubRepoLink {

    UUID projectId;
    String repoOwner;
    String repoName;
    UUID linkedByKeycloakId;
    String defaultBranch;
    boolean showReadme;
    LocalDateTime createdAt;
}
