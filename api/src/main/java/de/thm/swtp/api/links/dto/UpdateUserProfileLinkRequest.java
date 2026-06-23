package de.thm.swtp.api.links.dto;


import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateUserProfileLinkRequest(@Size(max = 100) String label, @Size(max = 300) @URL String url) {
}
