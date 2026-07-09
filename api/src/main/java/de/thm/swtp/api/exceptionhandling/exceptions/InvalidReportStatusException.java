package de.thm.swtp.api.exceptionhandling.exceptions;

public class InvalidReportStatusException extends RuntimeException {
    public InvalidReportStatusException(String message) {
        super(message);
    }
}
