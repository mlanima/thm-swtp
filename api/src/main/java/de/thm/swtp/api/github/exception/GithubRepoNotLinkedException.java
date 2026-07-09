package de.thm.swtp.api.github.exception;

import java.util.UUID;

/** Thrown when a project has no linked GitHub repository. */
public class GithubRepoNotLinkedException extends RuntimeException {

    public GithubRepoNotLinkedException(UUID projectId) {
        super("No GitHub repository linked to project: " + projectId);
    }
}
