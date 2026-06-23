package de.thm.swtp.api.userFollow.exception;

public class UserAlreadyFollowingException extends RuntimeException {
    public UserAlreadyFollowingException() {
        super("Already following this user.");
    }
}
