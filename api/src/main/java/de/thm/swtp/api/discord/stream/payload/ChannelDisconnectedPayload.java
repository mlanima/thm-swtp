package de.thm.swtp.api.discord.stream.payload;

import java.util.Map;

/** Carries the channel ID and reason when the bot loses access to a Discord channel. */
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
