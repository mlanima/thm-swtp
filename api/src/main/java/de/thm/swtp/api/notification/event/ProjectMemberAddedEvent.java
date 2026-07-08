package de.thm.swtp.api.notification.event;

import java.util.UUID;

public record ProjectMemberAddedEvent(UUID projectId, UUID newMemberKeycloakId) {}
