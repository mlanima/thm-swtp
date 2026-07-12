package de.thm.swtp.api.discord.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.thm.swtp.api.discord.entity.LinkedChannelEntity;

import java.util.UUID;

/**
 * API response payload for a linked Discord channel.
 * The optional {@code warning} field carries non-fatal hints (e.g. "invite expired").
 */
public record DiscordChannelResponse(
        UUID id,
        String discordChannelId,
        String discordGuildId,
        boolean isActive,
        String discordInviteUrl,
        @JsonInclude(JsonInclude.Include.NON_NULL) String warning
) {
    /**
     * Builds a response from the entity with no additional warning.
     */
    public static DiscordChannelResponse from(LinkedChannelEntity entity) {
        return from(entity, null);
    }

    /**
     * Builds a response from the entity, optionally attaching a warning message.
     */
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
