package de.thm.swtp.api.projectJoinRequest.mapper;

import de.thm.swtp.api.projectJoinRequest.domain.ProjectJoinRequest;
import de.thm.swtp.api.projectJoinRequest.entity.ProjectJoinRequestEntity;

public class ProjectJoinRequestMapper {

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
