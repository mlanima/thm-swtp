package de.thm.swtp.api.discord.dto;

/**
 * Quick status overview for a Discord channel: whether the bot is connected,
 * and how many messages were synced or failed today.
 */
public record DiscordStatusResponse(
        boolean isActive,
        String channelId,
        long syncedToday,
        long failedToday
) {}
