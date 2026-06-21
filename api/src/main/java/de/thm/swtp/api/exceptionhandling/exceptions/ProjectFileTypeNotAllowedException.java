package de.thm.swtp.api.exceptionhandling.exceptions;

public class ProjectFileTypeNotAllowedException extends RuntimeException {
    public ProjectFileTypeNotAllowedException(String mimeType) {
        super("File type not allowed: " + mimeType);
    }
}