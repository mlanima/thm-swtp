package de.thm.swtp.api.projectFiles.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ProjectFile {

    private UUID id;
    private UUID projectId;
    private String originalName;
    private String mimeType;
    private long sizeBytes;
    private LocalDateTime createdAt;
}