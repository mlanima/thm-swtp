package de.thm.swtp.api.links.dto;

import de.thm.swtp.api.links.domain.LinkVisibility;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateProjectLinkRequest(@Size(max = 100) String label, @Size(max = 300) @URL String url, LinkVisibility visibility) {
}
