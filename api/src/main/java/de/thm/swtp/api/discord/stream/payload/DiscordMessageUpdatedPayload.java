package de.thm.swtp.api.discord.stream.payload;

import java.util.Map;

/** Carries the updated content and message ID for a Discord edit event. */
public record DiscordMessageUpdatedPayload(
        String discordMsgId,
        String content
) {
    public static DiscordMessageUpdatedPayload from(Map<String, String> map) {
        return new DiscordMessageUpdatedPayload(
                map.get("discordMsgId"),
                map.get("content")
        );
    }
}
