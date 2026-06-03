package de.thm.swtp.api.projectLinks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateProjectLinkRequest(@Size(max = 100) String label, @Size(max = 300) @URL String url) {
}
