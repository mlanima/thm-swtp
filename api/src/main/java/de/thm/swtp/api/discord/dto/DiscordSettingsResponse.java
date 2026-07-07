package de.thm.swtp.api.discord.dto;

import de.thm.swtp.api.discord.entity.DiscordChannelSettingsEntity;

public record DiscordSettingsResponse(
        boolean notifyPostCreated,
        boolean notifyPostUpdated,
        boolean notifyPostDeleted,
        boolean notifyMemberJoin,
        boolean notifyMemberLeave
) {
    public static DiscordSettingsResponse from(DiscordChannelSettingsEntity entity) {
        return new DiscordSettingsResponse(
                entity.isNotifyPostCreated(),
                entity.isNotifyPostUpdated(),
                entity.isNotifyPostDeleted(),
                entity.isNotifyMemberJoin(),
                entity.isNotifyMemberLeave()
        );
    }

    public record UpdateRequest(
            boolean notifyPostCreated,
            boolean notifyPostUpdated,
            boolean notifyPostDeleted,
            boolean notifyMemberJoin,
            boolean notifyMemberLeave
    ) {}
}
