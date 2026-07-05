package de.thm.swtp.api.links.dto;

import de.thm.swtp.api.links.domain.ProjectReadme;

public record ProjectReadmeResponse(String repoUrl, String content, String owner, String repo) {

    public static ProjectReadmeResponse toResponse(ProjectReadme projectReadme) {
        return new ProjectReadmeResponse(
                projectReadme.getRepoUrl(),
                projectReadme.getContent(),
                projectReadme.getOwner(),
                projectReadme.getRepo());
    }
}
