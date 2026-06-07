package de.thm.swtp.api.projectInvitation.mapper;

import de.thm.swtp.api.projectInvitation.domain.ProjectInvite;
import de.thm.swtp.api.projectInvitation.entity.ProjectInviteEntity;

/** Maps between {@link ProjectInviteEntity} and {@link ProjectInvite} domain objects.*/
public class ProjectInviteMapper {

    /** Converts a project invitation entity into a domain object.*/
    public static ProjectInvite toDomain(ProjectInviteEntity projectInviteEntity) {
        return ProjectInvite.builder()
                .id(projectInviteEntity.getId())
                .projectId(projectInviteEntity.getProject().getId())
                .projectName(projectInviteEntity.getProject().getName())
                .projectUrl(projectInviteEntity.getProject().getProjectUrl())
                .invitedByUsername(projectInviteEntity.getProject().getOwner().getUsername())
                .invitedUserId(projectInviteEntity.getInvitedUser().getKeycloakId())
                .message(projectInviteEntity.getMessage())
                .createdAt(projectInviteEntity.getCreatedAt())
                .status(projectInviteEntity.getStatus())
                .build();
    }
}
