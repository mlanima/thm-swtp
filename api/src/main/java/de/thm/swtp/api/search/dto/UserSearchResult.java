package de.thm.swtp.api.search.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UserSearchResult(
        UUID keycloakId,
        String username,
        String title,
        String location
) {}
