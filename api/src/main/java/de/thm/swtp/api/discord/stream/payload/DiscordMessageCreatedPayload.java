package de.thm.swtp.api.discord.stream.payload;

import java.util.Map;

/** Carries the details of a new Discord message that should become a platform post. */
public record DiscordMessageCreatedPayload(
        String discordMsgId,
        String channelId,
        String content,
        String discordUserId,
        String discordUsername,
        String attachmentUrls
) {
    public static DiscordMessageCreatedPayload from(Map<String, String> map) {
        return new DiscordMessageCreatedPayload(
                map.get("discordMsgId"),
                map.get("channelId"),
                map.get("content"),
                map.get("discordUserId"),
                map.get("discordUsername"),
                map.get("attachmentUrls")
        );
    }
}
