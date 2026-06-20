package de.thm.swtp.api.exceptionhandling.exceptions;

public class UserProfileLinkNotFoundException extends RuntimeException {
    public UserProfileLinkNotFoundException(String message) {
        super(message);
    }
}
