package de.thm.swtp.api.discord.dto;

import de.thm.swtp.api.discord.entity.DiscordChannelSettingsEntity;

/**
 * API response payload for a channel's notification toggle settings.
 * Also contains the nested {@link UpdateRequest} used when changing those toggles.
 */
public record DiscordSettingsResponse(
        boolean notifyPostCreated,
        boolean notifyPostUpdated,
        boolean notifyPostDeleted,
        boolean notifyMemberJoin,
        boolean notifyMemberLeave
) {
    /**
     * Maps entity fields into the response DTO.
     */
    public static DiscordSettingsResponse from(DiscordChannelSettingsEntity entity) {
        return new DiscordSettingsResponse(
                entity.isNotifyPostCreated(),
                entity.isNotifyPostUpdated(),
                entity.isNotifyPostDeleted(),
                entity.isNotifyMemberJoin(),
                entity.isNotifyMemberLeave()
        );
    }

    /**
     * Request body for updating a channel's notification preferences.
     */
    public record UpdateRequest(
            boolean notifyPostCreated,
            boolean notifyPostUpdated,
            boolean notifyPostDeleted,
            boolean notifyMemberJoin,
            boolean notifyMemberLeave
    ) {}
}
