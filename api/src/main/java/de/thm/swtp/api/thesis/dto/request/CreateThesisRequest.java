package de.thm.swtp.api.thesis.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateThesisRequest(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 2000) String description,
        @Size(max = 300) String shortDescription,
        String thesisUrl,
        Set<String> tags
) {}
