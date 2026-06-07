package de.thm.swtp.api.project.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.*;
import java.util.*;


@Data
@Builder

public class ProjectResponse {
    private UUID id;
    private String name;
    private String description;
    private String projectUrl;
    @JsonProperty("isPrivateProject")
    private boolean isPrivateProject;
    private UUID ownerId;
    private Set<UUID> memberIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long favoriteCount;
}
