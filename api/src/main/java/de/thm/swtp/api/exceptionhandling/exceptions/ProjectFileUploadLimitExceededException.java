package de.thm.swtp.api.exceptionhandling.exceptions;

public class ProjectFileUploadLimitExceededException extends RuntimeException {
    public ProjectFileUploadLimitExceededException(int limit) {
        super("Upload limit reached. A project may not have more than " + limit + " files.");
    }
}