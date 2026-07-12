package de.thm.swtp.api.thesis.exception;

import java.util.UUID;

public class ThesisStudentAlreadyAssignedElsewhereException extends RuntimeException {
    public ThesisStudentAlreadyAssignedElsewhereException(UUID studentId) {
        super("Student \"" + studentId + "\" ist bereits einer anderen Abschlussarbeit zugewiesen.");
    }
}
