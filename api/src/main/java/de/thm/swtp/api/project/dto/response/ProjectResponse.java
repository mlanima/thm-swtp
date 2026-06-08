package de.thm.swtp.api.project.dto.response;

import lombok.*;
import java.time.*;
import java.util.*;


@Data
@Builder

public class ProjectResponse {
    private UUID id;
    private String name;
    private String description;
    private String shortDescription;
    private String projectUrl;
    private boolean isPrivateProject;
    private boolean allowJoinRequests;
    private UUID ownerId;
    private Set<UUID> memberIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ProjectStatsResponse stats;
    private long favoriteCount;
}
