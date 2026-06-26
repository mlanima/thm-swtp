package de.thm.swtp.api.thesis.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Builder
@Value
public class Thesis {
    UUID id;
    String title;
    String thesisUrl;
    String description;
    String shortDescription;
    UUID supervisorKeycloakId;
    Set<String> tags;
    Set<UUID> studentKeycloakIds;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
