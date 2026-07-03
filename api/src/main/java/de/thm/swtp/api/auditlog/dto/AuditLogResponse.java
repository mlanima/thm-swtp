package de.thm.swtp.api.auditlog.dto;

import de.thm.swtp.api.auditlog.AuditLogAction;
import de.thm.swtp.api.auditlog.AuditLogEntity;
import de.thm.swtp.api.auditlog.AuditLogTargetType;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        AuditLogAction action,
        UUID actorUserId,
        String actorUsername,
        String actorEmail,
        AuditLogTargetType targetType,
        UUID targetId,
        String targetName,
        String details,
        LocalDateTime createdAt
) {
    public static AuditLogResponse toResponse(AuditLogEntity entity) {
        return new AuditLogResponse(
                entity.getId(),
                entity.getAction(),
                entity.getActorUserId(),
                entity.getActorUsername(),
                entity.getActorEmail(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getTargetName(),
                entity.getDetails(),
                entity.getCreatedAt()
        );
    }
}