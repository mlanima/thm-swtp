package de.thm.swtp.api.exceptionhandling.exceptions;

import java.util.UUID;

public class ProjectFileNotFoundException extends RuntimeException {
    public ProjectFileNotFoundException(UUID fileId) {
        super("Project file not found: " + fileId);
    }
}