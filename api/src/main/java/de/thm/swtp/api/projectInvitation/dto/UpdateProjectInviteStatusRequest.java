package de.thm.swtp.api.projectInvitation.dto;

import de.thm.swtp.api.projectInvitation.domain.ProjectInviteStatus;
import jakarta.validation.constraints.NotNull;

/** Request DTO for accepting or rejecting a project invitation.*/
public record UpdateProjectInviteStatusRequest(@NotNull ProjectInviteStatus status) {}
