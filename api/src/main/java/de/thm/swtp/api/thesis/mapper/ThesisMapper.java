package de.thm.swtp.api.thesis.mapper;

import de.thm.swtp.api.thesis.ThesisEntity;
import de.thm.swtp.api.thesis.domain.Thesis;
import de.thm.swtp.api.tag.entity.TagEntity;
import de.thm.swtp.api.userprofile.entity.UserProfile;

import java.util.stream.Collectors;

public class ThesisMapper {

    public static Thesis toDomain(ThesisEntity entity) {
        return Thesis.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .thesisUrl(entity.getThesisUrl())
                .description(entity.getDescription())
                .shortDescription(entity.getShortDescription())
                .supervisorKeycloakId(entity.getSupervisor().getKeycloakId())
                .tags(entity.getTags().stream()
                        .map(TagEntity::getName)
                        .collect(Collectors.toSet()))
                .studentKeycloakIds(entity.getStudents().stream()
                        .map(UserProfile::getKeycloakId)
                        .collect(Collectors.toSet()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
