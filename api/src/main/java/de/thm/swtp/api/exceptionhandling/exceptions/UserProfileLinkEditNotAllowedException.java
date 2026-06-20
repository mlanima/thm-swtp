package de.thm.swtp.api.exceptionhandling.exceptions;

public class UserProfileLinkEditNotAllowedException extends RuntimeException {
    public UserProfileLinkEditNotAllowedException(String message) {
        super(message);
    }
}
