package de.thm.swtp.api.discord.stream.payload;

import java.util.Map;

public record InviteResponsePayload(
        String inviteId,
        String response
) {
    public static InviteResponsePayload from(Map<String, String> map) {
        return new InviteResponsePayload(
                map.get("inviteId"),
                map.get("response")
        );
    }
}
