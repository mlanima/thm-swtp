package de.thm.swtp.api.projectFavorite.exception;

import java.util.UUID;

public class ProjectFavoriteNotFoundException extends RuntimeException {
    public ProjectFavoriteNotFoundException(UUID projectId) {
        super("Project " + projectId + " is not in favorites.");
    }
}
