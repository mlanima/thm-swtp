package de.thm.swtp.api.projectFavorite.exception;

import java.util.UUID;

public class ProjectAlreadyFavoritedException extends RuntimeException {
    public ProjectAlreadyFavoritedException(UUID projectId) {
        super("Project " + projectId + " is already in favorites.");
    }
}
