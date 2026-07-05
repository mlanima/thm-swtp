package de.thm.swtp.api.search.mapper;

import de.thm.swtp.api.search.dto.UserSearchResult;
import de.thm.swtp.api.tag.entity.TagEntity;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import org.springframework.stereotype.Component;

/**
 * Maps {@link UserProfile} to the {@link UserSearchResult} DTO.
 * Extracts the fields relevant for search results and flattens the tag names.
 */
@Component
public class UserSearchMapper {

    /**
     * Converts a user profile entity into a search result DTO.
     *
     * @param user the entity to map
     * @return a {@link UserSearchResult} with keycloakId, username, title, location, and tag names
     */
    public UserSearchResult toResponse(UserProfile user) {
        return UserSearchResult.builder()
                .keycloakId(user.getKeycloakId())
                .username(user.getUsername())
                .title(user.getTitle())
                .location(user.getLocation())
                .tags(user.getTags().stream().map(TagEntity::getName).toList())
                .build();
    }
}
