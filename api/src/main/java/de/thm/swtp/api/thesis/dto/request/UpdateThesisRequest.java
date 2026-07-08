package de.thm.swtp.api.thesis.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateThesisRequest {
    @Size(max = 100) private String title;
    @Size(max = 2000) private String description;
    @Size(max = 300) private String shortDescription;
    private String thesisUrl;
    private Set<String> tags;
}
