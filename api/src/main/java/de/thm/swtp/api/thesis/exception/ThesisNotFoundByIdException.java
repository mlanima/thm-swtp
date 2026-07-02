package de.thm.swtp.api.thesis.exception;

import java.util.UUID;

public class ThesisNotFoundByIdException extends RuntimeException {
    public ThesisNotFoundByIdException(UUID id) {
        super("Keine Abschlussarbeit mit der ID \"" + id + "\" gefunden.");
    }
}
