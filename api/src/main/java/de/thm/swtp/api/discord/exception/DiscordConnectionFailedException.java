package de.thm.swtp.api.discord.exception;

/** Thrown when the API cannot reach Discord or the bot interaction fails. */
public class DiscordConnectionFailedException extends RuntimeException {
    public DiscordConnectionFailedException(String message) {
        super(message);
    }
}
