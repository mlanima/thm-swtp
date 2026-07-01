package de.thm.swtp.api.professorRequest.dto;

import de.thm.swtp.api.professorRequest.domain.ProfessorRequest;
import de.thm.swtp.api.professorRequest.domain.ProfessorRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** Response DTO for professor-rights requests. */
public record ProfessorRequestResponse(
        UUID id,
        UUID requestingUserId,
        String requestingUsername,
        String email,
        String text,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime verificationExpiresAt,
        LocalDateTime emailVerifiedAt,
        ProfessorRequestStatus status
) {

    /** Converts a professor-request domain object into a response DTO. */
    public static ProfessorRequestResponse toResponse(ProfessorRequest request) {
        return new ProfessorRequestResponse(
                request.getId(),
                request.getRequestingUserId(),
                request.getRequestingUsername(),
                request.getEmail(),
                request.getText(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                request.getVerificationExpiredAt(),
                request.getEmailVerifiedAt(),
                request.getStatus()
        );
    }
}
