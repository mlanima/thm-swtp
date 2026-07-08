package de.thm.swtp.api.discord.exception;

public class DiscordChannelNotFoundException extends RuntimeException {
    public DiscordChannelNotFoundException(String message) {
        super(message);
    }
}
