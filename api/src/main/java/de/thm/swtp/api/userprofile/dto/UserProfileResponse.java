package de.thm.swtp.api.userprofile.dto;

import lombok.Builder;
import java.util.UUID;

@Builder
public record UserProfileResponse(
        UUID keycloakId,
        String username,
        String email,
        String title,
        String location,
        int followers,
        String about,
        String experience,
        boolean isProfessor
) {}
