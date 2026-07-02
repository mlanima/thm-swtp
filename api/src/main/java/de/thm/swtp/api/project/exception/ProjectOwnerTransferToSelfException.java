package de.thm.swtp.api.project.exception;

import java.util.UUID;

public class ProjectOwnerTransferToSelfException extends RuntimeException {
    public ProjectOwnerTransferToSelfException(UUID projectId) {
        super("Eigentumsübertragung fehlgeschlagen: Der neue Eigentümer des Projekts " + projectId + " ist bereits der aktuelle Eigentümer.");
    }
}
