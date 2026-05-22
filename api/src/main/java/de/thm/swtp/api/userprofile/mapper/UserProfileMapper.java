package de.thm.swtp.api.userprofile.mapper;

import de.thm.swtp.api.userprofile.dto.UserProfileResponse;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    public UserProfileResponse toResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .userId(profile.getUser().getKeycloakId())
                .about(profile.getAbout())
                .experience(profile.getExperience())
                .build();
    }
}
