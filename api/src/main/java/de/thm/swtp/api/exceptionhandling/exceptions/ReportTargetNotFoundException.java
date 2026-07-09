package de.thm.swtp.api.exceptionhandling.exceptions;

public class ReportTargetNotFoundException extends RuntimeException {
    public ReportTargetNotFoundException(String message) {
        super(message);
    }
}
