package de.thm.swtp.api.professorRequest.exception;

/** Thrown when an operation is attempted on a professor-rights request with an invalid status. */
public class ProfessorRequestInvalidStatusException extends RuntimeException {

    public ProfessorRequestInvalidStatusException(String message) {
        super(message);
    }
}
