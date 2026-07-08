package de.thm.swtp.api.professorRequest.domain;

/** Represents the lifecycle of a professor-rights request. */
public enum ProfessorRequestStatus {
    WAITING_EMAIL_VERIFICATION,
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED
}
