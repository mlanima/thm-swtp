package de.thm.swtp.api.exceptionhandling.exceptions;

public class ProfileAccessDeniedException extends RuntimeException {

    public ProfileAccessDeniedException() {
        super("You are not allowed to modify another user's profile");
    }
}
