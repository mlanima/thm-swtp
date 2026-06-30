package de.thm.swtp.api.exceptionhandling;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse extends ExceptionResponse {

    private final String error;
    private final String errorCode;
    private final Instant timestamp;

    private ErrorResponse(int statusCode, String error, String message) {
        this(statusCode, error, message, null);
    }

    private ErrorResponse(int statusCode, String error, String message, String errorCode) {
        super(statusCode, message);
        this.error = error;
        this.errorCode = errorCode;
        this.timestamp = Instant.now();
    }

    public static ErrorResponse of(int statusCode, String error, String message) {
        return new ErrorResponse(statusCode, error, message);
    }

    public static ErrorResponse of(int statusCode, String error, String message, String errorCode) {
        return new ErrorResponse(statusCode, error, message, errorCode);
    }

    public String getError() {
        return error;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
