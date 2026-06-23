package de.thm.swtp.api.professorRequest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request DTO for creating a new professor-rights request. */
public record CreateProfessorRequestRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 1000) String text
) {
}
