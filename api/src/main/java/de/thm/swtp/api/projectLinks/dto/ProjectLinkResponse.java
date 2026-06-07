package de.thm.swtp.api.projectLinks.dto;

import de.thm.swtp.api.projectLinks.domain.ProjectLink;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectLinkResponse(UUID id, UUID projectId, String label, String url, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static ProjectLinkResponse toResponse(ProjectLink projectLink) {
        return new ProjectLinkResponse(
                projectLink.getId(),
                projectLink.getProjectId(),
                projectLink.getLabel(),
                projectLink.getUrl(),
                projectLink.getCreatedAt(),
                projectLink.getUpdatedAt()
        );
    }
}
