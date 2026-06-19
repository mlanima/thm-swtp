package de.thm.swtp.api.projectFiles.mapper;

import de.thm.swtp.api.projectFiles.domain.ProjectFile;
import de.thm.swtp.api.projectFiles.entity.ProjectFileEntity;

public class ProjectFileMapper {

    public static ProjectFile toDomain(ProjectFileEntity entity) {
        return ProjectFile.builder()
                .id(entity.getId())
                .projectId(entity.getProject().getId())
                .originalName(entity.getOriginalName())
                .mimeType(entity.getMimeType())
                .sizeBytes(entity.getSizeBytes())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}