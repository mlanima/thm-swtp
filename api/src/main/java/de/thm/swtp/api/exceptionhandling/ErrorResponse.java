package de.thm.swtp.api.exceptionhandling;

import java.time.Instant;

public class ErrorResponse extends ExceptionResponse {

    private final String error;
    private final Instant timestamp;

    private ErrorResponse(int statusCode, String error, String message) {
        super(statusCode, message);
        this.error = error;
        this.timestamp = Instant.now();
    }

    public static ErrorResponse of(int statusCode, String error, String message) {
        return new ErrorResponse(statusCode, error, message);
    }

    public String getError() {
        return error;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
