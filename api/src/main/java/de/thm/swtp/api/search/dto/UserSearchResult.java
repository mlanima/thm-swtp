package de.thm.swtp.api.search.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for user search results.
 * <p>
 * Contains the fields that are relevant for displaying a user profile
 * in search results, including its tag names as a flat string list.
 *
 * @param keycloakId the user's unique Keycloak identifier
 * @param username   the display username
 * @param title      the user's professional title
 * @param location   the user's location
 * @param tags       list of tag names assigned to the user
 */
@Builder
public record UserSearchResult(
        UUID keycloakId,
        String username,
        String title,
        String location,
        List<String> tags
) {}
