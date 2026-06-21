package de.thm.swtp.api.exceptionhandling.exceptions;

public class ProjectPostAccessDeniedException extends RuntimeException {
    public ProjectPostAccessDeniedException(String message) {
        super(message);
    }
}
