package de.thm.swtp.api.projectInvitation.mapper;

import de.thm.swtp.api.projectInvitation.domain.ProjectInvite;
import de.thm.swtp.api.projectInvitation.entity.ProjectInviteEntity;

public class ProjectInviteMapper {

    public static ProjectInvite toDomain(ProjectInviteEntity projectInviteEntity) {
        return ProjectInvite.builder()
                .id(projectInviteEntity.getId())
                .projectId(projectInviteEntity.getProject().getId())
                //.invitedUserId(projectInviteEntity) UserEntity is still missing
                .message(projectInviteEntity.getMessage())
                .sendDate(projectInviteEntity.getSendDate())
                .status(projectInviteEntity.getStatus())
                .build();
    }
}
