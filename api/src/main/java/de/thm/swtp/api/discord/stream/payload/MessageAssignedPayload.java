package de.thm.swtp.api.discord.stream.payload;

import java.util.Map;
import java.util.UUID;

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
