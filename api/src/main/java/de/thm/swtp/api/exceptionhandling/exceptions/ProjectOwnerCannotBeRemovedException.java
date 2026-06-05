package de.thm.swtp.api.exceptionhandling.exceptions;

import java.util.UUID;

public class ProjectOwnerCannotBeRemovedException extends RuntimeException {
    public ProjectOwnerCannotBeRemovedException(UUID ownerId, UUID projectID) {
        super("Project owner: " + ownerId + " cannot be removed from his own project: " + projectID );
    }
}
