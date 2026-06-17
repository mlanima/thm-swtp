package de.thm.swtp.api.links.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ProjectLink {

    private UUID id;
    private UUID projectId;
    private String label;
    private String url;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
