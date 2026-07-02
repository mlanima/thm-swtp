package de.thm.swtp.api.thesis.exception;

public class ThesisUrlGenerationFailedException extends RuntimeException {
    public ThesisUrlGenerationFailedException(String baseSlug) {
        super("Für den Slug \"" + baseSlug + "\" konnte keine eindeutige Thesis-URL generiert werden.");
    }
}
