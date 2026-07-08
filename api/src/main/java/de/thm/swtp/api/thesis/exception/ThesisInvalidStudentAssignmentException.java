package de.thm.swtp.api.thesis.exception;

import java.util.UUID;

public class ThesisInvalidStudentAssignmentException extends RuntimeException {
    public ThesisInvalidStudentAssignmentException(UUID studentId) {
        super("Nutzer \"" + studentId + "\" kann nicht als Student zugewiesen werden.");
    }
}
