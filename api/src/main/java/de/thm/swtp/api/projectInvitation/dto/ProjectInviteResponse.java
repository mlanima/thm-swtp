package de.thm.swtp.api.projectInvitation.dto;

import de.thm.swtp.api.projectInvitation.domain.ProjectInvite;
import de.thm.swtp.api.projectInvitation.domain.ProjectInviteStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** Response DTO for project invitations.*/
public record ProjectInviteResponse(
        UUID id,
        UUID projectId,
        String projectName,
        String projectUrl,
        String invitedByUsername,
        UUID invitedUserId,
        String message,
        ProjectInviteStatus status,
        LocalDateTime createdAt,
        String discordInviteUrl
) {

    /** Converts a project invitation domain object into a response DTO.*/
    public static ProjectInviteResponse toResponse(ProjectInvite invite, String discordInviteUrl){
        return new ProjectInviteResponse(
              invite.getId(),
              invite.getProjectId(),
              invite.getProjectName(),
              invite.getProjectUrl(),
              invite.getInvitedByUsername(),
              invite.getInvitedUserId(),
              invite.getMessage(),
              invite.getStatus(),
              invite.getCreatedAt(),
              discordInviteUrl
        );
    }

    public static ProjectInviteResponse toResponse(ProjectInvite invite){
        return toResponse(invite, null);
    }
}
