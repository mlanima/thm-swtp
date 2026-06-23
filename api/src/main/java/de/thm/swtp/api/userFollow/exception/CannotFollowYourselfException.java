package de.thm.swtp.api.userFollow.exception;

public class CannotFollowYourselfException extends RuntimeException {
    public CannotFollowYourselfException() {
        super("Cannot follow yourself.");
    }
}
