package de.thm.swtp.api.projectGithubRepo.dto;

import de.thm.swtp.api.projectGithubRepo.domain.GithubReadme;

public record GithubReadmeResponse(
        String repoOwner,
        String repoName,
        String defaultBranch,
        String markdown,
        boolean available) {

    public static GithubReadmeResponse toResponse(GithubReadme readme) {
        return new GithubReadmeResponse(
                readme.getRepoOwner(),
                readme.getRepoName(),
                readme.getDefaultBranch(),
                readme.getMarkdown(),
                readme.isAvailable());
    }
}
