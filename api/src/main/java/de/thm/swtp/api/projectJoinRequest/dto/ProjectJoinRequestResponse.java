package de.thm.swtp.api.projectJoinRequest.dto;

import de.thm.swtp.api.projectJoinRequest.domain.ProjectJoinRequest;
import de.thm.swtp.api.projectJoinRequest.domain.ProjectJoinRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectJoinRequestResponse(UUID id, UUID projectId, UUID requestingUser, String message,
                                         LocalDateTime createdAt, LocalDateTime updatedAt, ProjectJoinRequestStatus status) {

    public static ProjectJoinRequestResponse toResponse(ProjectJoinRequest joinRequest) {
        return new ProjectJoinRequestResponse(
                joinRequest.getId(),
                joinRequest.getProjectId(),
                joinRequest.getRequestingUserId(),
                joinRequest.getMessage(),
                joinRequest.getCreatedAt(),
                joinRequest.getUpdatedAt(),
                joinRequest.getStatus()
        );
    }
}

