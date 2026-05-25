package de.thm.swtp.api.search.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ProjectSearchResult(
        UUID id,
        String name,
        String description
) {}
