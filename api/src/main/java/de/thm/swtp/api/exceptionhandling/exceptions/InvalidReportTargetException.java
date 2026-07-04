package de.thm.swtp.api.exceptionhandling.exceptions;

public class InvalidReportTargetException extends RuntimeException {
    public InvalidReportTargetException(String message) {
        super(message);
    }
}
