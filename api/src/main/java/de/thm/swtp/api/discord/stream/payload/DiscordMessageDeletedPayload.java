package de.thm.swtp.api.discord.stream.payload;

import java.util.Map;

/** Carries just the Discord message ID for a deletion event. */
public record DiscordMessageDeletedPayload(
        String discordMsgId
) {
    public static DiscordMessageDeletedPayload from(Map<String, String> map) {
        return new DiscordMessageDeletedPayload(map.get("discordMsgId"));
    }
}
