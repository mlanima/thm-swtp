package de.thm.swtp.api.discord.dto;

public record DiscordStatusResponse(
        boolean isActive,
        String channelId,
        long syncedToday,
        long failedToday
) {}
