package de.thm.swtp.api.userFollow.exception;

public class UserFollowNotFoundException extends RuntimeException {
    public UserFollowNotFoundException() {
        super("Follow relationship not found.");
    }
}
