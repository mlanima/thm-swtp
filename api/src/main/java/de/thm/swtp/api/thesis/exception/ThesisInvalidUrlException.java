package de.thm.swtp.api.thesis.exception;

public class ThesisInvalidUrlException extends RuntimeException {
    public ThesisInvalidUrlException(String url) {
        super("Ungültige Abschlussarbeit-URL: \"" + url + "\". Erlaubt sind nur Kleinbuchstaben, Ziffern und Bindestriche (3–30 Zeichen).");
    }
}
