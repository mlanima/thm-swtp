package de.thm.swtp.api.auditlog.domain;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditActorTest {

    @Test
    void fromJwt_shouldUsePreferredUsernameAndEmail() {
        UUID userId = UUID.randomUUID();

        Jwt jwt = jwtWithClaims(Map.of(
                "sub", userId.toString(),
                "preferred_username", "moderator",
                "email", "moderator@test.de"
        ));

        AuditActor actor = AuditActor.fromJwt(jwt);

        assertThat(actor.userId()).isEqualTo(userId);
        assertThat(actor.username()).isEqualTo("moderator");
        assertThat(actor.email()).isEqualTo("moderator@test.de");
    }

    @Test
    void fromJwt_shouldFallbackToName_whenPreferredUsernameIsMissing() {
        UUID userId = UUID.randomUUID();

        Jwt jwt = jwtWithClaims(Map.of(
                "sub", userId.toString(),
                "name", "Moderator Name",
                "email", "moderator@test.de"
        ));

        AuditActor actor = AuditActor.fromJwt(jwt);

        assertThat(actor.userId()).isEqualTo(userId);
        assertThat(actor.username()).isEqualTo("Moderator Name");
        assertThat(actor.email()).isEqualTo("moderator@test.de");
    }

    @Test
    void fromJwt_shouldFallbackToUnknownUser_whenNoUsernameClaimExists() {
        UUID userId = UUID.randomUUID();

        Jwt jwt = jwtWithClaims(Map.of(
                "sub", userId.toString(),
                "email", "moderator@test.de"
        ));

        AuditActor actor = AuditActor.fromJwt(jwt);

        assertThat(actor.userId()).isEqualTo(userId);
        assertThat(actor.username()).isEqualTo("Unknown user");
        assertThat(actor.email()).isEqualTo("moderator@test.de");
    }

    private Jwt jwtWithClaims(Map<String, Object> claims) {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                claims
        );
    }
}