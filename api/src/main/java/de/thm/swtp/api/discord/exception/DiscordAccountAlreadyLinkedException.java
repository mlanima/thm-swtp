package de.thm.swtp.api.discord.exception;

public class DiscordAccountAlreadyLinkedException extends RuntimeException {
    public DiscordAccountAlreadyLinkedException(String message) {
        super(message);
    }
}
