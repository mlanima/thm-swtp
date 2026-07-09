package de.thm.swtp.api.discord.exception;

public class DiscordConnectionFailedException extends RuntimeException {
    public DiscordConnectionFailedException(String message) {
        super(message);
    }
}
