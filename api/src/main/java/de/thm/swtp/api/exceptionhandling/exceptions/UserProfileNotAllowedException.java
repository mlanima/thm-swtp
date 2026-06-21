package de.thm.swtp.api.exceptionhandling.exceptions;

public class UserProfileNotAllowedException extends RuntimeException {
    public UserProfileNotAllowedException(String message) {
        super(message);
    }
}
