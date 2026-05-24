package de.thm.swtp.api.exceptionhandling;

public abstract class ExceptionResponse {

    protected int statusCode;
    protected String message;

    protected ExceptionResponse(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }
}
