package de.thm.swtp.api.github.exception;

/** Thrown when the linking user's GitHub account has no push/admin access to the repository —
 * they must own it or be a collaborator with write access to link it to a project. */
public class GithubRepoAccessDeniedException extends RuntimeException {

    public GithubRepoAccessDeniedException() {
        super("You must have push access to this GitHub repository to link it");
    }
}
