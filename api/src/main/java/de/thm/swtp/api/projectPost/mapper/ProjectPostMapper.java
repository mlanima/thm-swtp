package de.thm.swtp.api.projectPost.mapper;

import de.thm.swtp.api.projectPost.domain.ProjectPost;
import de.thm.swtp.api.projectPost.entity.ProjectPostEntity;

public class ProjectPostMapper {

    public static ProjectPost toDomain(ProjectPostEntity projectPostEntity) {
        return ProjectPost.builder()
                .id(projectPostEntity.getId())
                .projectId(projectPostEntity.getProject().getId())
                .authorId(projectPostEntity.getAuthor().getKeycloakId())
                .authorName(projectPostEntity.getAuthor().getUsername())
                .title(projectPostEntity.getTitle())
                .content(projectPostEntity.getContent())
                .status(projectPostEntity.getStatus())
                .contentFormat(projectPostEntity.getContentFormat())
                .publishedAt(projectPostEntity.getPublishedAt())
                .archivedAt(projectPostEntity.getArchivedAt())
                .createdAt(projectPostEntity.getCreatedAt())
                .updatedAt(projectPostEntity.getUpdatedAt())
                .build();
    }
}
