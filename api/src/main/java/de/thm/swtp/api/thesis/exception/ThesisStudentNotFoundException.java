package de.thm.swtp.api.thesis.exception;

import java.util.UUID;

public class ThesisStudentNotFoundException extends RuntimeException {
    public ThesisStudentNotFoundException(UUID studentId, UUID thesisId) {
        super("Student " + studentId + " is not assigned to thesis " + thesisId);
    }
}
