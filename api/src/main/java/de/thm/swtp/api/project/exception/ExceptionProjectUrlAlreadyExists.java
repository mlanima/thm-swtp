package de.thm.swtp.api.project.exception;

public class ExceptionProjectUrlAlreadyExists extends RuntimeException {
    public ExceptionProjectUrlAlreadyExists(String url) {
        super("Die Projekt-URL \"" + url + "\" ist bereits vergeben.");
    }
}