package de.thm.swtp.api.notification.event;

import java.util.UUID;

public record ThesisStudentAddedEvent(UUID thesisId, UUID studentKeycloakId) {}
