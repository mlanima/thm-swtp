package de.thm.swtp.api.links.dto;

import de.thm.swtp.api.links.domain.UserProfileLink;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserProfileLinkResponse(UUID id, UUID userProfileId, String label, String url, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static UserProfileLinkResponse toResponse(UserProfileLink userProfileLink) {
        return new UserProfileLinkResponse(
                userProfileLink.getId(),
                userProfileLink.getUserProfileId(),
                userProfileLink.getLabel(),
                userProfileLink.getUrl(),
                userProfileLink.getCreatedAt(),
                userProfileLink.getUpdatedAt()
        );
    }
}
