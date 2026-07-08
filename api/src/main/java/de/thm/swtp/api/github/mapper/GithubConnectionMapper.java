package de.thm.swtp.api.github.mapper;

import de.thm.swtp.api.github.domain.GithubConnection;
import de.thm.swtp.api.github.entity.GithubConnectionEntity;

public class GithubConnectionMapper {

    public static GithubConnection toDomain(GithubConnectionEntity entity) {
        return GithubConnection.builder()
                .keycloakId(entity.getKeycloakId())
                .githubUserId(entity.getGithubUserId())
                .githubLogin(entity.getGithubLogin())
                .avatarUrl(entity.getAvatarUrl())
                .scopes(entity.getScopes())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
