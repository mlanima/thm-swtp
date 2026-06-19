package de.thm.swtp.api.exceptionhandling.exceptions;

public class ProjectFileDoesNotBelongToProjectException extends RuntimeException {
    public ProjectFileDoesNotBelongToProjectException() {
        super("The file does not belong to this project.");
    }
}