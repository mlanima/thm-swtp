package de.thm.swtp.api.projectLinks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record CreateProjectLinkRequest(@NotBlank @Size(max = 100) String label, @NotBlank @Size(max = 300) @URL String url) {
}
