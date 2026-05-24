package de.thm.swtp.api.exceptionhandling.exceptions;

public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException() {
        super("You are not allowed to modify another user's profile");
    }
}
