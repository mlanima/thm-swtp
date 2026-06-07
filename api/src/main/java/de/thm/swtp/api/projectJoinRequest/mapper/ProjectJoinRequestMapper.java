package de.thm.swtp.api.projectJoinRequest.mapper;

import de.thm.swtp.api.projectJoinRequest.domain.ProjectJoinRequest;
import de.thm.swtp.api.projectJoinRequest.entity.ProjectJoinRequestEntity;

/** Maps between {@link ProjectJoinRequestEntity} and {@link ProjectJoinRequest} domain objects. */
public class ProjectJoinRequestMapper {

    /** Converts a project join-request entity into a domain object. */
    public static ProjectJoinRequest toDomain(ProjectJoinRequestEntity entity) {
        return ProjectJoinRequest.builder()
                .id(entity.getId())
                .projectId(entity.getProject().getId())
                .requestingUserId(entity.getRequestingUser().getKeycloakId())
                .message(entity.getMessage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .status(entity.getStatus())
                .build();
    }
}
