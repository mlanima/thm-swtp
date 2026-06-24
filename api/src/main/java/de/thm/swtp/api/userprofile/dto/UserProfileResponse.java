package de.thm.swtp.api.userprofile.dto;

import de.thm.swtp.api.userprofile.domain.UserStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record UserProfileResponse(
        UUID keycloakId,
        String username,
        String email,
        String title,
        String location,
        int followers,
        String about,
        String experience,
        boolean isProfessor,
        UserStatus status,
        String banReason,
        LocalDateTime bannedAt
) {}
