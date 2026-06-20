package de.thm.swtp.api.exceptionhandling.exceptions;

public class UserProfileLinkDoesNotBelongToProfileException extends RuntimeException {
    public UserProfileLinkDoesNotBelongToProfileException(String message) {
        super(message);
    }
}
