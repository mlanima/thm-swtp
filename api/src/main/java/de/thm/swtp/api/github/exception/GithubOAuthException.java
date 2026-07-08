package de.thm.swtp.api.github.exception;

/** Thrown when the GitHub OAuth code-exchange fails or GitHub reports an OAuth error. */
public class GithubOAuthException extends RuntimeException {

    public GithubOAuthException(String message) {
        super(message);
    }
}
