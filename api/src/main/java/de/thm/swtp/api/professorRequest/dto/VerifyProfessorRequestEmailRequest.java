package de.thm.swtp.api.professorRequest.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyProfessorRequestEmailRequest(@NotBlank String token) {
}
