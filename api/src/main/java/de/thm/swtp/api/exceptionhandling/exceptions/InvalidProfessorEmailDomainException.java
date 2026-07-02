package de.thm.swtp.api.exceptionhandling.exceptions;

public class InvalidProfessorEmailDomainException extends RuntimeException {
    public InvalidProfessorEmailDomainException() {
        super("Professor requests require a valid THM email address");
    }
}
