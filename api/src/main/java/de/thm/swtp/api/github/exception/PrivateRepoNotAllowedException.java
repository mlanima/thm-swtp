package de.thm.swtp.api.github.exception;

/** Thrown when a user tries to link a private GitHub repository — the repo card is served
 * unauthenticated once a linker disconnects, so only public repos can be linked. */
public class PrivateRepoNotAllowedException extends RuntimeException {

    public PrivateRepoNotAllowedException() {
        super("Only public GitHub repositories can be linked to a project");
    }
}
