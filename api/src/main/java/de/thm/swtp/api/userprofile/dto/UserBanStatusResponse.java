package de.thm.swtp.api.userprofile.dto;

import java.time.LocalDateTime;

public record UserBanStatusResponse(
        boolean banned,
        String banReason,
        LocalDateTime bannedAt
) {}
