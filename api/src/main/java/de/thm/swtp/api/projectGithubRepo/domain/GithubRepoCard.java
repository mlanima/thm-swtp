package de.thm.swtp.api.projectGithubRepo.domain;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class GithubRepoCard {

    GithubRepoLink link;
    GithubRepoData data;
    boolean dataUnavailable;
}
