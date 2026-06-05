package de.thm.swtp.api.exceptionhandling.exceptions;

import java.util.UUID;

public class ProjectMemberNotFoundException extends RuntimeException {
    public ProjectMemberNotFoundException(UUID memberId, UUID projectId) {
        super("Member " + memberId + " does not belong to the project " + projectId);
    }
}
