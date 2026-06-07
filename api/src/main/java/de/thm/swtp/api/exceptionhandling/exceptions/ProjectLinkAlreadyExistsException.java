package de.thm.swtp.api.exceptionhandling.exceptions;

public class ProjectLinkAlreadyExistsException extends RuntimeException {
    public ProjectLinkAlreadyExistsException() {
        super("The link already exists for this project.");
    }
}
