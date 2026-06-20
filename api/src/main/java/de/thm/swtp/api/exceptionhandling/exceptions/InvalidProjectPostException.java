package de.thm.swtp.api.exceptionhandling.exceptions;

public class InvalidProjectPostException extends RuntimeException {
    public InvalidProjectPostException(String message) {
        super(message);
    }
}
