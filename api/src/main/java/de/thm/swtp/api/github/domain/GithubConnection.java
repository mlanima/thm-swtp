package de.thm.swtp.api.github.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/** A user's connected GitHub account. Deliberately holds no access token — that stays
 * encrypted in the entity and is only ever decrypted inside {@code GithubConnectionService}. */
@Builder
@Value
public class GithubConnection {

    UUID keycloakId;
    long githubUserId;
    String githubLogin;
    String avatarUrl;
    String scopes;
    GithubConnectionStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
