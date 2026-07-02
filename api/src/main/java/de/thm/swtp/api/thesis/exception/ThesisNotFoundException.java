package de.thm.swtp.api.thesis.exception;

public class ThesisNotFoundException extends RuntimeException {
    public ThesisNotFoundException(String thesisUrl) {
        super("Keine Abschlussarbeit mit der URL \"" + thesisUrl + "\" gefunden.");
    }
}
