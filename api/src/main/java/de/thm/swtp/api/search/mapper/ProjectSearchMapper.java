package de.thm.swtp.api.search.mapper;

import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.search.dto.ProjectSearchResult;
import de.thm.swtp.api.tag.entity.TagEntity;
import org.springframework.stereotype.Component;

/**
 * Maps {@link ProjectEntity} to the {@link ProjectSearchResult} DTO.
 * Extracts the fields relevant for search results and flattens the tag names.
 */
@Component
public class ProjectSearchMapper {

    /**
     * Converts a project entity into a search result DTO.
     *
     * @param project the entity to map
     * @return a {@link ProjectSearchResult} with id, name, description, projectUrl, and tag names
     */
    public ProjectSearchResult toResponse(ProjectEntity project) {
        return ProjectSearchResult.builder()
                .id(project.getId())
                .name(project.getName())
                .shortDescription(project.getShortDescription())
                .description(project.getDescription())
                .projectUrl(project.getProjectUrl())
                .tags(project.getTags().stream().map(TagEntity::getName).toList())
                .build();
    }
}
