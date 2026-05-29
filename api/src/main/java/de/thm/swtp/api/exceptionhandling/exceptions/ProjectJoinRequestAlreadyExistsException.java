package de.thm.swtp.api.exceptionhandling.exceptions;

import java.util.UUID;

public class ProjectJoinRequestAlreadyExistsException extends RuntimeException {
    public ProjectJoinRequestAlreadyExistsException(UUID projectId) {
        super("A join request has already been created for the project: " + projectId);
    }
}
