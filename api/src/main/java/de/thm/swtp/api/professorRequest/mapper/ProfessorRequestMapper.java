package de.thm.swtp.api.professorRequest.mapper;

import de.thm.swtp.api.professorRequest.domain.ProfessorRequest;
import de.thm.swtp.api.professorRequest.entity.ProfessorRequestEntity;

/** Maps between {@link ProfessorRequestEntity} and {@link ProfessorRequest} domain objects. */
public class ProfessorRequestMapper {

    /** Converts a professor-request entity into a domain object. */
    public static ProfessorRequest toDomain(ProfessorRequestEntity entity) {
        return ProfessorRequest.builder()
                .id(entity.getId())
                .requestingUserId(entity.getRequestingUser().getKeycloakId())
                .requestingUsername(entity.getRequestingUser().getUsername())
                .email(entity.getEmail())
                .text(entity.getText())
                .emailVerifiedAt(entity.getEmailVerifiedAt())
                .verificationExpiresAt(entity.getVerificationExpiresAt())
                .verificationTokenHash(entity.getVerificationTokenHash())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .status(entity.getStatus())
                .build();
    }
}
