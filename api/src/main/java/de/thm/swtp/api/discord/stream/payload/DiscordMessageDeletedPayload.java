package de.thm.swtp.api.discord.stream.payload;

import java.util.Map;

public record DiscordMessageDeletedPayload(
        String discordMsgId
) {
    public static DiscordMessageDeletedPayload from(Map<String, String> map) {
        return new DiscordMessageDeletedPayload(map.get("discordMsgId"));
    }
}
