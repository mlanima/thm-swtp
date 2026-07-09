package de.thm.swtp.api.github.exception;

/** Thrown when a project action requires the acting user to have an active GitHub connection
 * (e.g. linking a repository), but none exists or it has been marked invalid. */
public class GithubConnectionRequiredException extends RuntimeException {

    public GithubConnectionRequiredException() {
        super("Connect your GitHub account before linking a repository");
    }
}
