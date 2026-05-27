package de.thm.swtp.api.tag.exception;

public class TagAccessDeniedException extends RuntimeException {
    public TagAccessDeniedException(String message) {
        super(message);
    }
}
