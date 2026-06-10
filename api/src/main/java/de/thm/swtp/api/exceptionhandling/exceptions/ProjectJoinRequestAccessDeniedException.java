package de.thm.swtp.api.exceptionhandling.exceptions;

public class ProjectJoinRequestAccessDeniedException extends RuntimeException {
    public ProjectJoinRequestAccessDeniedException(String message) {
        super(message);
    }
}
