package de.thm.swtp.api.discord.dto;

import de.thm.swtp.api.discord.entity.LinkedChannelEntity;

import java.util.UUID;

public record DiscordChannelResponse(
        UUID id,
        String discordChannelId,
        String discordGuildId,
        boolean isActive,
        String discordInviteUrl
) {
    public static DiscordChannelResponse from(LinkedChannelEntity entity) {
        return new DiscordChannelResponse(
                entity.getId(),
                entity.getDiscordChannelId(),
                entity.getDiscordGuildId(),
                entity.isActive(),
                entity.getDiscordInviteUrl()
        );
    }
}
