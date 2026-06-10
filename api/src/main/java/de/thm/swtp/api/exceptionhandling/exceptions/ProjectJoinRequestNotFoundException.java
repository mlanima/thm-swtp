package de.thm.swtp.api.exceptionhandling.exceptions;

import java.util.UUID;

public class ProjectJoinRequestNotFoundException extends RuntimeException {
    public ProjectJoinRequestNotFoundException(UUID requestId) {
        super("Join request not found for requestId: " + requestId);
    }
}
