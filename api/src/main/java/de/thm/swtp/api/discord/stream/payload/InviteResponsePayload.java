package de.thm.swtp.api.discord.stream.payload;

import java.util.Map;

/** Carries the invite ID and the user's decision (accept / decline) from Discord. */
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
