package de.thm.swtp.api.exceptionhandling.exceptions;

public class ProjectJoinRequestAlreadyExistsException extends RuntimeException {
    public ProjectJoinRequestAlreadyExistsException(String message) {
        super(message);
    }
}
