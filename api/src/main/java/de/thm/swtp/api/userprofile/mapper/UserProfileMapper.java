package de.thm.swtp.api.userprofile.mapper;

import de.thm.swtp.api.userprofile.dto.UserProfileResponse;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    public UserProfileResponse toResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .keycloakId(profile.getKeycloakId())
                .username(profile.getUsername())
                .email(profile.getEmail())
                .title(profile.getTitle())
                .location(profile.getLocation())
                .followers(profile.getFollowers())
                .about(profile.getAbout())
                .experience(profile.getExperience())
                .isProfessor(profile.isProfessor())
                .status(profile.getStatus())
                .build();
    }
}
