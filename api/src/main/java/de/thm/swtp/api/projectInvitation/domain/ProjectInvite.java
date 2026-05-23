package de.thm.swtp.api.projectInvitation.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Value
public class ProjectInvite {
    UUID id;
    UUID projectId;
    UUID invitedUserId;
    String message;
    LocalDateTime sendDate;
    ProjectInviteStatus status;
}
