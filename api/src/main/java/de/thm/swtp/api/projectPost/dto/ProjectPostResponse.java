package de.thm.swtp.api.projectPost.dto;

import de.thm.swtp.api.projectPost.domain.PostContentFormat;
import de.thm.swtp.api.projectPost.domain.ProjectPost;
import de.thm.swtp.api.projectPost.domain.ProjectPostStatus;


import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectPostResponse(UUID id, UUID projectId, UUID authorId,
                                  String authorName, String title, String content, String imageUrl,
                                  PostContentFormat contentFormat, ProjectPostStatus status,
                                  LocalDateTime publishedAt, LocalDateTime archivedAt,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static ProjectPostResponse toResponse(ProjectPost projectPost) {
        return new ProjectPostResponse(
                projectPost.getId(),
                projectPost.getProjectId(),
                projectPost.getAuthorId(),
                projectPost.getAuthorName(),
                projectPost.getTitle(),
                projectPost.getContent(),
                projectPost.getImageUrl(),
                projectPost.getContentFormat(),
                projectPost.getStatus(),
                projectPost.getPublishedAt(),
                projectPost.getArchivedAt(),
                projectPost.getCreatedAt(),
                projectPost.getUpdatedAt()
        );
    }
}
