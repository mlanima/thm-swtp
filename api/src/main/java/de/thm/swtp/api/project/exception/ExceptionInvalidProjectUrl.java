package de.thm.swtp.api.project.exception;

public class ExceptionInvalidProjectUrl extends RuntimeException {
    public ExceptionInvalidProjectUrl(String url) {
        super("Ungültige Projekt-URL: \"" + url + "\". Nur Kleinbuchstaben, Zahlen und Bindestriche sind erlaubt (3–30 Zeichen).");
    }
}