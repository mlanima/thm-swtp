package de.thm.swtp.api.project.exception;

import java.util.*;

public class ExceptionOwnerNotFound extends RuntimeException {

    public ExceptionOwnerNotFound(UUID ownerId){
        super("Kein Benutzer mit der ID" + ownerId + "gefunden");

    }
}
