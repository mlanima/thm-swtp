package de.thm.swtp.api.project.exception;

public class ExceptionProjectResponse extends RuntimeException {
    public ExceptionProjectResponse(String projectUrl) {
        super("Ein Projekt mit der URL '" + projectUrl + "' existiert bereits.");
    }
}
