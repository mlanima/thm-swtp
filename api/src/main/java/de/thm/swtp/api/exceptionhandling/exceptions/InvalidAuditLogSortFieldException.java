package de.thm.swtp.api.exceptionhandling.exceptions;

public class InvalidAuditLogSortFieldException extends RuntimeException {
    public InvalidAuditLogSortFieldException(String field) {
        super("Invalid audit log sort field: " + field);
    }
}