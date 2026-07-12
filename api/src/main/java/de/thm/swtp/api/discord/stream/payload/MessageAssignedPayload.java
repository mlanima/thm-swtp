package de.thm.swtp.api.discord.stream.payload;

import java.util.Map;
import java.util.UUID;

/** Carries the mapping between a platform post and its newly mirrored Discord message. */
public record MessageAssignedPayload(
        UUID postId,
        String discordMsgId,
        String channelId,
        String guildId
) {
    public static MessageAssignedPayload from(Map<String, String> map) {
        return new MessageAssignedPayload(
                UUID.fromString(map.get("postId")),
                map.get("discordMsgId"),
                map.get("channelId"),
                map.get("guildId")
        );
    }
}
