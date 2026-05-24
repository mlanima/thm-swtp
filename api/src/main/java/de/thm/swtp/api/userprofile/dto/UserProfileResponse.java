package de.thm.swtp.api.userprofile.dto;

import lombok.Builder;

@Builder
public record UserProfileResponse(
        String keycloakId,
        String username,
        String email,
        String about,
        String experience
) {}
