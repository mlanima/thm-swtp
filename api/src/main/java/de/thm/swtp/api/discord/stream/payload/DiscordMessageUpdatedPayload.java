package de.thm.swtp.api.discord.stream.payload;

import java.util.Map;

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
