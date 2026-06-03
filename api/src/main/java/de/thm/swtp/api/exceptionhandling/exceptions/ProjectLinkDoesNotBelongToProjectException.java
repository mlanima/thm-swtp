package de.thm.swtp.api.exceptionhandling.exceptions;

public class ProjectLinkDoesNotBelongToProjectException extends RuntimeException {
    public ProjectLinkDoesNotBelongToProjectException() {
        super("The given link does not belong to this project.");
    }
}
