package de.thm.swtp.api.location.exception;

public class GooglePlacesApiException extends RuntimeException {

    public GooglePlacesApiException(final String message) {
        super(message);
    }
}
