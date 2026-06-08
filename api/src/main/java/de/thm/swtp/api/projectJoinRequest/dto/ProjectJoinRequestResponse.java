package de.thm.swtp.api.projectJoinRequest.dto;

import de.thm.swtp.api.projectJoinRequest.domain.ProjectJoinRequest;
import de.thm.swtp.api.projectJoinRequest.domain.ProjectJoinRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** Response DTO for project join-requests. */
public record ProjectJoinRequestResponse(UUID id, UUID projectId, UUID requestingUser, String requestingUsername,
                                         String message, LocalDateTime createdAt, LocalDateTime updatedAt,
                                         ProjectJoinRequestStatus status) {

    /** Converts a project join-request domain object into a response DTO. */
    public static ProjectJoinRequestResponse toResponse(ProjectJoinRequest joinRequest) {
        return new ProjectJoinRequestResponse(
                joinRequest.getId(),
                joinRequest.getProjectId(),
                joinRequest.getRequestingUserId(),
                joinRequest.getRequestingUsername(),
                joinRequest.getMessage(),
                joinRequest.getCreatedAt(),
                joinRequest.getUpdatedAt(),
                joinRequest.getStatus()
        );
    }
}

