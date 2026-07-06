package de.thm.swtp.api.github.exception;

/** Thrown when the GitHub API is unreachable or returns an unexpected error. */
public class GithubApiException extends RuntimeException {

    public GithubApiException(String message) {
        super(message);
    }
}
