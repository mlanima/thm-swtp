package de.thm.swtp.api.projectFiles.dto;

import de.thm.swtp.api.projectFiles.domain.FileVisibility;
import de.thm.swtp.api.projectFiles.domain.ProjectFile;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectFileResponse(UUID id, UUID projectId, String originalName, String mimeType, long sizeBytes,
                                   LocalDateTime createdAt, FileVisibility visibility) {

    public static ProjectFileResponse toResponse(ProjectFile file) {
        return new ProjectFileResponse(
                file.getId(),
                file.getProjectId(),
                file.getOriginalName(),
                file.getMimeType(),
                file.getSizeBytes(),
                file.getCreatedAt(),
                file.getVisibility()
        );
    }
}
