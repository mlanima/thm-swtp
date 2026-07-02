package de.thm.swtp.api.thesis.dto.response;

import de.thm.swtp.api.thesis.domain.Thesis;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class ThesisResponse {
    private UUID id;
    private String title;
    private String thesisUrl;
    private String description;
    private String shortDescription;
    private UUID supervisorKeycloakId;
    private String supervisorUsername;
    private Set<String> tags;
    private Set<UUID> studentKeycloakIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ThesisResponse toResponse(Thesis thesis) {
        return ThesisResponse.builder()
                .id(thesis.getId())
                .title(thesis.getTitle())
                .thesisUrl(thesis.getThesisUrl())
                .description(thesis.getDescription())
                .shortDescription(thesis.getShortDescription())
                .supervisorKeycloakId(thesis.getSupervisorKeycloakId())
                .supervisorUsername(thesis.getSupervisorUsername())
                .tags(thesis.getTags())
                .studentKeycloakIds(thesis.getStudentKeycloakIds())
                .createdAt(thesis.getCreatedAt())
                .updatedAt(thesis.getUpdatedAt())
                .build();
    }
}
