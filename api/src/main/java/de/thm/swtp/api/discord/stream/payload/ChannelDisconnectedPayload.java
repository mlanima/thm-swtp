package de.thm.swtp.api.discord.stream.payload;

import java.util.Map;

public record ChannelDisconnectedPayload(
        String channelId,
        String reason
) {
    public static ChannelDisconnectedPayload from(Map<String, String> map) {
        return new ChannelDisconnectedPayload(
                map.get("channelId"),
                map.get("reason")
        );
    }
}
