package de.thm.swtp.api.auditlog.domain;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public record AuditActor(
        UUID userId,
        String username,
        String email
) {
    public static AuditActor fromJwt(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");

        if (username == null || username.isBlank()) {
            username = jwt.getClaimAsString("name");
        }

        if (username == null || username.isBlank()) {
            username = "Unknown user";
        }

        return new AuditActor(userId, username, email);
    }
}
