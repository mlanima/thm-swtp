package de.thm.swtp.api.links.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record CreateUserProfileLinkRequest(@NotBlank @Size(max = 100) String label, @NotBlank @Size(max = 300) @URL String url) {
}
