package de.thm.swtp.api.github.exception;

/** Thrown when the OAuth {@code state} parameter returned by GitHub is missing, tampered with,
 * expired, or does not belong to the current user. */
public class InvalidGithubStateException extends RuntimeException {

    public InvalidGithubStateException(String message) {
        super(message);
    }
}
