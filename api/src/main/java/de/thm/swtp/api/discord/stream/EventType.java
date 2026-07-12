package de.thm.swtp.api.discord.stream;

/** Event types that can travel through the inbound Redis stream from the Discord bot. */
public enum EventType {
    DISCORD_MESSAGE_CREATED,
    DISCORD_MESSAGE_UPDATED,
    DISCORD_MESSAGE_DELETED,
    DISCORD_MESSAGE_ASSIGNED,
    INVITE_RESPONSE,
    CHANNEL_DISCONNECTED
}
