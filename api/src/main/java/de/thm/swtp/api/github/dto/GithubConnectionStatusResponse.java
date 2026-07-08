package de.thm.swtp.api.github.dto;

import de.thm.swtp.api.github.domain.GithubConnection;

public record GithubConnectionStatusResponse(
        boolean enabled,
        boolean connected,
        String githubLogin,
        String avatarUrl,
        String status) {

    public static GithubConnectionStatusResponse toResponse(boolean enabled, GithubConnection connection) {
        if (connection == null) {
            return new GithubConnectionStatusResponse(enabled, false, null, null, null);
        }
        return new GithubConnectionStatusResponse(
                enabled, true, connection.getGithubLogin(), connection.getAvatarUrl(), connection.getStatus().name());
    }
}
