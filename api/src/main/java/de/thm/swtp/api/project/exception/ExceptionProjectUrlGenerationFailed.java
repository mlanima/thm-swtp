package de.thm.swtp.api.project.exception;

public class ExceptionProjectUrlGenerationFailed extends RuntimeException {
    public ExceptionProjectUrlGenerationFailed(String baseSlug) {
        super("Für den Slug \"" + baseSlug + "\" konnte keine eindeutige Projekt-URL generiert werden.");
    }
}