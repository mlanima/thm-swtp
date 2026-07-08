package de.thm.swtp.api.github.entity;

import de.thm.swtp.api.github.domain.GithubConnectionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/** A user's connected GitHub account, keyed by their Keycloak id (one connection per user). */
@Entity
@Table(name = "github_connections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GithubConnectionEntity {

    @Id
    @Column(nullable = false)
    private UUID keycloakId;

    @Column(nullable = false)
    private Long githubUserId;

    @Column(nullable = false, length = 100)
    private String githubLogin;

    @Column(length = 300)
    private String avatarUrl;

    @Column(nullable = false, length = 512)
    private String encryptedAccessToken;

    @Column(length = 200)
    private String scopes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GithubConnectionStatus status = GithubConnectionStatus.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
