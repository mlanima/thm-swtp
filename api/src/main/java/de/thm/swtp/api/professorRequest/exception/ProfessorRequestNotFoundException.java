package de.thm.swtp.api.professorRequest.exception;

import java.util.UUID;

/** Thrown when a professor-rights request is not found. */
public class ProfessorRequestNotFoundException extends RuntimeException {

    public ProfessorRequestNotFoundException(UUID id) {
        super("Professor request not found with id: " + id);
    }
}
