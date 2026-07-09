package de.thm.swtp.api.github.exception;

import java.util.UUID;

/** Thrown when the README section is requested for a project whose owner hasn't opted in to
 * showing it (even though a repo is linked). */
public class GithubReadmeNotEnabledException extends RuntimeException {

    public GithubReadmeNotEnabledException(UUID projectId) {
        super("README display is not enabled for project: " + projectId);
    }
}
