package de.thm.swtp.api.discord.exception;

/** Thrown when someone tries to link a Discord account that is already tied to another user. */
public class DiscordAccountAlreadyLinkedException extends RuntimeException {
    public DiscordAccountAlreadyLinkedException(String message) {
        super(message);
    }
}
