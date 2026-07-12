package de.thm.swtp.api.discord.exception;

/** Thrown when a requested Discord channel is not found or has never been linked. */
public class DiscordChannelNotFoundException extends RuntimeException {
    public DiscordChannelNotFoundException(String message) {
        super(message);
    }
}
