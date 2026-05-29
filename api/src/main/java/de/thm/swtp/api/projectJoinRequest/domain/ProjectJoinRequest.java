package de.thm.swtp.api.projectJoinRequest.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Value
public class ProjectJoinRequest {

    UUID id;
    UUID projectId;
    UUID requestingUserId;
    String message;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    ProjectJoinRequestStatus status;
}
