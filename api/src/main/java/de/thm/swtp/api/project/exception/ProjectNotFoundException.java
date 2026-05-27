package de.thm.swtp.api.project.exception;

import java.util.UUID;

public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(UUID projectId){
        super("Project not found: " + projectId);
    }
}
