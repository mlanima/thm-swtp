package de.thm.swtp.api.project.exception;

public class InvalidProjectUrlException extends RuntimeException {
    public InvalidProjectUrlException(String url) {
        super("Ungültige Projekt-URL: \"" + url + "\". Nur Kleinbuchstaben, Zahlen und Bindestriche sind erlaubt (3–30 Zeichen).");
    }
}