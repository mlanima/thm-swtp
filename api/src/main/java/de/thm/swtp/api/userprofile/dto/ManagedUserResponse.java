package de.thm.swtp.api.userprofile.dto;

import de.thm.swtp.api.userprofile.domain.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ManagedUserResponse(
        UUID keycloakId,
        String username,
        String email,
        boolean isProfessor,
        UserStatus status,
        String banReason,
        LocalDateTime bannedAt,
        LocalDateTime createdAt
) {}
