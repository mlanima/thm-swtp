package de.thm.swtp.api.github.exception;

/** Thrown when a GitHub OAuth endpoint is called but no client id/secret/encryption key is
 * configured — the integration self-disables rather than failing at startup. */
public class GithubIntegrationDisabledException extends RuntimeException {

    public GithubIntegrationDisabledException() {
        super("GitHub integration is not configured");
    }
}
