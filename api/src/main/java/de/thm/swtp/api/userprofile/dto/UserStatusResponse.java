package de.thm.swtp.api.userprofile.dto;

import java.time.LocalDateTime;

public record UserStatusResponse(
        boolean banned,
        String banReason,
        LocalDateTime bannedAt
) {}
