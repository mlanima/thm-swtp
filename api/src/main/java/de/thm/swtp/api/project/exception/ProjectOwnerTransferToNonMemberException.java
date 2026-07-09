package de.thm.swtp.api.project.exception;

import java.util.UUID;

public class ProjectOwnerTransferToNonMemberException extends RuntimeException {
    public ProjectOwnerTransferToNonMemberException(UUID newOwnerId, UUID projectId) {
        super("Eigentumsübertragung fehlgeschlagen: Benutzer " + newOwnerId + " ist kein Mitglied des Projekts " + projectId + ".");
    }
}
