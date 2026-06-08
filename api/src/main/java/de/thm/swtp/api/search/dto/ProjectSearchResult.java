package de.thm.swtp.api.search.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for project search results.
 * <p>
 * Contains the fields that are relevant for displaying a project
 * in search results, including its tag names as a flat string list.
 *
 * @param id          the project's unique identifier
 * @param name        the project name
 * @param description a short description of the project
 * @param projectUrl  the project's URL slug
 * @param tags        list of tag names assigned to the project
 */
@Builder
public record ProjectSearchResult(
        UUID id,
        String name,
        String shortDescription,
        String description,
        String projectUrl,
        List<String> tags
) {}
