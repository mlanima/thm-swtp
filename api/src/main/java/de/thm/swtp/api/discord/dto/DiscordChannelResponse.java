package de.thm.swtp.api.discord.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.thm.swtp.api.discord.entity.LinkedChannelEntity;

import java.util.UUID;

public record DiscordChannelResponse(
        UUID id,
        String discordChannelId,
        String discordGuildId,
        boolean isActive,
        String discordInviteUrl,
        @JsonInclude(JsonInclude.Include.NON_NULL) String warning
) {
    public static DiscordChannelResponse from(LinkedChannelEntity entity) {
        return from(entity, null);
    }

    public static DiscordChannelResponse from(LinkedChannelEntity entity, String warning) {
        return new DiscordChannelResponse(
                entity.getId(),
                entity.getDiscordChannelId(),
                entity.getDiscordGuildId(),
                entity.isActive(),
                entity.getDiscordInviteUrl(),
                warning
        );
    }
}
