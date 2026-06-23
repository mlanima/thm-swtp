package de.thm.swtp.api.links.mapper;

import de.thm.swtp.api.links.domain.ProjectLink;
import de.thm.swtp.api.links.entity.ProjectLinkEntity;

public class ProjectLinkMapper{

    public static ProjectLink toDomain(ProjectLinkEntity projectLinkEntity) {
        return ProjectLink.builder()
                .id(projectLinkEntity.getId())
                .projectId(projectLinkEntity.getProject().getId())
                .label(projectLinkEntity.getLabel())
                .url(projectLinkEntity.getUrl())
                .createdAt(projectLinkEntity.getCreatedAt())
                .updatedAt(projectLinkEntity.getUpdatedAt())
                .visibility(projectLinkEntity.getVisibility())
                .build();
    }
}
