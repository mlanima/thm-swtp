package de.thm.swtp.api.links.mapper;


import de.thm.swtp.api.links.domain.UserProfileLink;
import de.thm.swtp.api.links.entity.UserProfileLinkEntity;

public class UserProfileLinkMapper {

    public static UserProfileLink toDomain(UserProfileLinkEntity userProfileLinkEntity) {
        return UserProfileLink.builder()
                .id(userProfileLinkEntity.getId())
                .userProfileId(userProfileLinkEntity.getUserProfile().getKeycloakId())
                .label(userProfileLinkEntity.getLabel())
                .url(userProfileLinkEntity.getUrl())
                .createdAt(userProfileLinkEntity.getCreatedAt())
                .updatedAt(userProfileLinkEntity.getUpdatedAt())
                .build();
    }
}
