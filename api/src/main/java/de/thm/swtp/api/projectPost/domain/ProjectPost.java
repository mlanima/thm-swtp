package de.thm.swtp.api.projectPost.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/** Domain object representing a post in a project.*/
@Builder
@Value
public class ProjectPost {
    UUID id;
    UUID projectId;
    UUID authorId;
    String title;
    String content;
    ProjectPostStatus status;
    PostContentFormat contentFormat;
    LocalDateTime publishedAt;
    LocalDateTime archivedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
