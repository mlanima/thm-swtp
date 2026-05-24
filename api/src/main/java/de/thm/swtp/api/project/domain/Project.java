package de.thm.swtp.api.project.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Builder
@Value
public class Project {
    UUID id;
    String name;
    String description;
    String projectUrl;
    boolean isPrivateProject;
    Set<UUID> memberIds;
    UUID ownerId;
    Set<UUID> tagIds;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
