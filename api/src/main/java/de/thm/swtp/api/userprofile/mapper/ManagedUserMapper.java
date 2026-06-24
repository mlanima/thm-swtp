package de.thm.swtp.api.userprofile.mapper;

import de.thm.swtp.api.userprofile.dto.ManagedUserResponse;
import de.thm.swtp.api.userprofile.entity.UserProfile;

public class ManagedUserMapper {

    public ManagedUserResponse toResponse(UserProfile userProfile) {
        return new ManagedUserResponse(
                userProfile.getKeycloakId(),
                userProfile.getUsername(),
                userProfile.getEmail(),
                userProfile.isProfessor(),
                userProfile.getStatus(),
                userProfile.getBanReason(),
                userProfile.getBannedAt()
        );
    }
}
