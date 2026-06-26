package de.thm.swtp.api.userprofile.mapper;

import de.thm.swtp.api.userprofile.domain.UserStatus;
import de.thm.swtp.api.userprofile.dto.UserStatusResponse;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class UserStatusMapper {

    public UserStatusResponse toBannedResponse(UserProfile userProfile) {
        boolean banned = userProfile.getStatus() == UserStatus.BANNED;
        return new UserStatusResponse(
                banned,
                banned ? userProfile.getBanReason() : null,
                banned ? userProfile.getBannedAt() : null
        );
    }

    public UserStatusResponse toNotBannedResponse() {
        return new UserStatusResponse(false, null, null);
    }
}
