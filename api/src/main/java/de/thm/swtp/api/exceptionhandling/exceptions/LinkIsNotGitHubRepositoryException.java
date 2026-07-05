package de.thm.swtp.api.exceptionhandling.exceptions;

public class LinkIsNotGitHubRepositoryException extends RuntimeException {
    public LinkIsNotGitHubRepositoryException() {
        super("The README can only be shown for links pointing to a GitHub repository.");
    }
}
