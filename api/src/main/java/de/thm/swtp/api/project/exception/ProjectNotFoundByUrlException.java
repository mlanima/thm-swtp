package de.thm.swtp.api.project.exception;

public class ProjectNotFoundByUrlException extends RuntimeException {
    public ProjectNotFoundByUrlException(String url) {
        super("No project found with URL: " + url);
    }
}
