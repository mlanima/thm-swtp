package de.thm.swtp.api.projectInvitation.exception;

import java.util.UUID;

public class ProjectInviteNotFoundException extends RuntimeException {

    public ProjectInviteNotFoundException(UUID inviteId) {
        super("Project invite not found: " + inviteId);
    }
}
