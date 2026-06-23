package de.thm.swtp.api.professorRequest.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Value
public class ProfessorRequest {

    UUID id;
    UUID requestingUserId;
    String requestingUsername;
    String name;
    String email;
    String text;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    ProfessorRequestStatus status;
}
