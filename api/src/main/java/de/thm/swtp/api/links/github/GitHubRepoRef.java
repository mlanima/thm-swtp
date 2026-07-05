package de.thm.swtp.api.links.github;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record GitHubRepoRef(String owner, String repo) {

    private static final Pattern GITHUB_REPO_URL = Pattern.compile(
            "^https?://(?:www\\.)?github\\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+?)(?:\\.git)?/?(?:[/?#].*)?$",
            Pattern.CASE_INSENSITIVE);

    public static Optional<GitHubRepoRef> parse(String url) {
        if (url == null) {
            return Optional.empty();
        }

        Matcher matcher = GITHUB_REPO_URL.matcher(url.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        return Optional.of(new GitHubRepoRef(matcher.group(1), matcher.group(2)));
    }
}
