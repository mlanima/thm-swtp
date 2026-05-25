package de.thm.swtp.api.userprofile.exception;

public class UserProfileNotFoundException extends RuntimeException {

    public UserProfileNotFoundException(String userId) {
        super("Profile not found for user: " + userId);
    }
}
