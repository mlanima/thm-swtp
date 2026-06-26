package de.thm.swtp.api.thesis.exception;

public class ThesisTitleAlreadyExistsException extends RuntimeException {
    public ThesisTitleAlreadyExistsException(String title) {
        super("Eine Abschlussarbeit mit dem Titel \"" + title + "\" existiert bereits.");
    }
}
