package de.thm.swtp.api.exceptionhandling.exceptions;

public class UserProfileLinkAlreadyExistsException extends RuntimeException {
    public UserProfileLinkAlreadyExistsException(String message) {
        super(message);
    }
}
