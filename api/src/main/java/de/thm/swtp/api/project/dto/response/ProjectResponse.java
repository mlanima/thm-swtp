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
    private String shortDescription;
    private String projectUrl;
    @JsonProperty("isPrivateProject")
    private boolean isPrivateProject;
    private boolean allowJoinRequests;
    private UUID ownerId;
    private String ownerUsername;
    private String ownerDiscordId;
    private String ownerDiscordUsername;
    private String discordChannelId;
    private String discordGuildId;
    private String discordInviteUrl;
    private Set<UUID> memberIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ProjectStatsResponse stats;
    private long favoriteCount;
}
