package de.thm.swtp.api.exceptionhandling.exceptions;

import java.util.UUID;

public class ProjectPostNotFoundException extends RuntimeException {
    public ProjectPostNotFoundException(UUID postId) {
        super("Project post not found: " + postId);
    }
}
