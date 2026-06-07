package de.thm.swtp.api.projectInvitation.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/** Represents an invitation to collaborate on a project.*/
@Builder
@Value
public class ProjectInvite {
    /** Unique Identifier.*/
    UUID id;

    /** Unique Identifier of the project.*/
    UUID projectId;

    /** Name of the project.*/
    String projectName;

    /** URL slug of the project.*/
    String projectUrl;

    /** Username of the project owner who sent the invitation.*/
    String invitedByUsername;

    /** Unique Identifier of the user which receives the invitation.*/
    UUID invitedUserId;

    /** Message the Project-Owner can define to the invited user.*/
    String message;

    /** Timestamp of when the invitation was created.*/
    LocalDateTime createdAt;

    /** Current status of the invitation.*/
    ProjectInviteStatus status;
}
