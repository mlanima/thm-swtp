package de.thm.swtp.api.projectGithubRepo.domain;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class GithubReadme {

    String repoOwner;
    String repoName;
    String defaultBranch;
    String markdown;
    boolean available;
}
