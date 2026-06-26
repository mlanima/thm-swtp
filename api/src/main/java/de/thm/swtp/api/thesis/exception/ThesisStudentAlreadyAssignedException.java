package de.thm.swtp.api.thesis.exception;

import java.util.UUID;

public class ThesisStudentAlreadyAssignedException extends RuntimeException {
    public ThesisStudentAlreadyAssignedException(UUID studentId, UUID thesisId) {
        super("Student " + studentId + " is already assigned to thesis " + thesisId);
    }
}
