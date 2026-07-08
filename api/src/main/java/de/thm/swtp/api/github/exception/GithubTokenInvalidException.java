package de.thm.swtp.api.github.exception;

/** Thrown when the GitHub API rejects a stored access token (HTTP 401) — the connection
 * should be marked invalid so the user is prompted to reconnect. */
public class GithubTokenInvalidException extends RuntimeException {

    public GithubTokenInvalidException(String message) {
        super(message);
    }
}
