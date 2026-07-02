package de.thm.swtp.api.thesis.exception;

public class ThesisUrlAlreadyExistsException extends RuntimeException {
    public ThesisUrlAlreadyExistsException(String url) {
        super("Eine Abschlussarbeit mit der URL \"" + url + "\" existiert bereits.");
    }
}
