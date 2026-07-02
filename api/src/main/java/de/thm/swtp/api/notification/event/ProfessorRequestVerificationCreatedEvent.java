package de.thm.swtp.api.notification.event;

import de.thm.swtp.api.professorRequest.domain.ProfessorRequest;

public record ProfessorRequestVerificationCreatedEvent(ProfessorRequest professorRequest, String verificationToken) {
}
