package de.thm.swtp.api.project.dto.response;

import de.thm.swtp.api.userprofile.entity.UserProfile;

import java.util.UUID;

public record ProjectMemberResponse(UUID keycloakId, String username, String email) {

    public static ProjectMemberResponse toResponse(UserProfile userProfile) {
        return new ProjectMemberResponse(
                userProfile.getKeycloakId(),
                userProfile.getUsername(),
                userProfile.getEmail()
        );

    }
}
