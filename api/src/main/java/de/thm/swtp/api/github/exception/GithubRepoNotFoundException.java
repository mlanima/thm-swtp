package de.thm.swtp.api.github.exception;

/** Thrown when a GitHub repository does not exist or is not visible to the calling token. */
public class GithubRepoNotFoundException extends RuntimeException {

    public GithubRepoNotFoundException(String owner, String repo) {
        super("GitHub repository not found: " + owner + "/" + repo);
    }
}
