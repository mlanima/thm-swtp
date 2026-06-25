package de.thm.swtp.api.professorRequest.exception;

import java.util.UUID;

/** Thrown when a user already has a pending professor-rights request. */
public class ProfessorRequestAlreadyExistsException extends RuntimeException {

    public ProfessorRequestAlreadyExistsException(UUID userId) {
        super("User " + userId + " already has a pending professor request.");
    }
}
