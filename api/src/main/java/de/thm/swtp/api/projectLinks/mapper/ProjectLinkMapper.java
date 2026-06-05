package de.thm.swtp.api.projectLinks.mapper;

import de.thm.swtp.api.projectLinks.domain.ProjectLink;
import de.thm.swtp.api.projectLinks.entity.ProjectLinkEntity;

public class ProjectLinkMapper{

    public static ProjectLink toDomain(ProjectLinkEntity projectLinkEntity) {
        return ProjectLink.builder()
                .id(projectLinkEntity.getId())
                .projectId(projectLinkEntity.getProject().getId())
                .label(projectLinkEntity.getLabel())
                .url(projectLinkEntity.getUrl())
                .createdAt(projectLinkEntity.getCreatedAt())
                .updatedAt(projectLinkEntity.getUpdatedAt())
                .build();
    }
}
