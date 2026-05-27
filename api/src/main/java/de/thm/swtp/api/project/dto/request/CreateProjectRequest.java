package de.thm.swtp.api.project.dto.request;

import java.util.Set;
import java.util.UUID;

public record CreateProjectRequest(
        String name,
        String description,
        String projectUrl,
        boolean isPrivateProject,
        Set<UUID> memberIds,
        Set<UUID> tagIds
) {}
