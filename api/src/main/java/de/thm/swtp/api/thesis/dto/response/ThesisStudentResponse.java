package de.thm.swtp.api.thesis.dto.response;

import de.thm.swtp.api.userprofile.entity.UserProfile;

import java.util.UUID;

public record ThesisStudentResponse(UUID keycloakId, String username, String email) {

    public static ThesisStudentResponse toResponse(UserProfile userProfile) {
        return new ThesisStudentResponse(
                userProfile.getKeycloakId(),
                userProfile.getUsername(),
                userProfile.getEmail()
        );
    }
}
