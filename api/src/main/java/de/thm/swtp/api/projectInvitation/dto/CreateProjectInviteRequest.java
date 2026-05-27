package de.thm.swtp.api.projectInvitation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Request DTO for creating a new project invitation.*/
public record CreateProjectInviteRequest(@NotNull UUID invitedUserId, @Size(max=500) String message) {}
