package de.thm.swtp.api.exceptionhandling.exceptions;

import java.util.UUID;

public class ProjectLinkNotFoundException extends RuntimeException {
    public ProjectLinkNotFoundException(UUID linkId) {
        super("Project link not found: " + linkId);
    }
}
