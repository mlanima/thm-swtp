package de.thm.swtp.api.exceptionhandling.exceptions;

public class ReportAlreadyExistsException extends RuntimeException {
    public ReportAlreadyExistsException(String message) {
        super(message);
    }
}
