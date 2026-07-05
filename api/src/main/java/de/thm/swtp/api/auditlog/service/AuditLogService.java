package de.thm.swtp.api.auditlog.service;

import de.thm.swtp.api.auditlog.domain.AuditActor;
import de.thm.swtp.api.auditlog.domain.AuditLogAction;
import de.thm.swtp.api.auditlog.domain.AuditLogTargetType;
import de.thm.swtp.api.auditlog.entity.AuditLogEntity;
import de.thm.swtp.api.auditlog.repository.AuditLogRepository;
import de.thm.swtp.api.exceptionhandling.exceptions.InvalidAuditLogSortFieldException;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    private static final Set<String> MANAGED_AUDIT_LOG_SORT_FIELDS = Set.of(
            "createdAt",
            "action",
            "actorUsername",
            "targetType",
            "targetName"
    );

    public Page<AuditLogEntity> getAuditLogs(Pageable pageable) {
        validateAuditLogSort(pageable);
        return auditLogRepository.findAll(pageable);
    }

    private void validateAuditLogSort(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!MANAGED_AUDIT_LOG_SORT_FIELDS.contains(order.getProperty())) {
                throw new InvalidAuditLogSortFieldException(order.getProperty());
            }
        }
    }

    public void logProjectDeleted(AuditActor actor, UUID projectId, String projectName) {
        createLog(
                AuditLogAction.PROJECT_DELETED,
                actor,
                AuditLogTargetType.PROJECT,
                projectId,
                projectName,
                "Project was deleted."
        );
    }

    public void logProjectPostDeleted(AuditActor actor, UUID postId, String postTitle, UUID projectId, String projectName) {
        createLog(
                AuditLogAction.PROJECT_POST_DELETED,
                actor,
                AuditLogTargetType.PROJECT_POST,
                postId,
                postTitle,
                projectName + " (" + projectId + ")"
        );
    }

    public void logUserBanned(AuditActor actor, UUID bannedUserId, String bannedUsername, String reason) {
        createLog(
                AuditLogAction.USER_BANNED,
                actor,
                AuditLogTargetType.USER,
                bannedUserId,
                bannedUsername,
                reason == null || reason.isBlank() ? "—" : reason
        );
    }

    public void logUserUnbanned(AuditActor actor, UUID unbannedUserId, String unbannedUsername) {
        createLog(
                AuditLogAction.USER_UNBANNED,
                actor,
                AuditLogTargetType.USER,
                unbannedUserId,
                unbannedUsername,
                null
        );
    }

    public void logProfessorRequestAccepted(AuditActor actor, UUID requestId, String requestingUsername, String requestingEmail) {
        createLog(
                AuditLogAction.PROFESSOR_REQUEST_ACCEPTED,
                actor,
                AuditLogTargetType.PROFESSOR_REQUEST,
                requestId,
                requestingUsername,
                requestingEmail
        );
    }

    public void logProfessorRequestRejected(AuditActor actor, UUID requestId, String requestingUsername, String requestingEmail) {
        createLog(
                AuditLogAction.PROFESSOR_REQUEST_REJECTED,
                actor,
                AuditLogTargetType.PROFESSOR_REQUEST,
                requestId,
                requestingUsername,
                requestingEmail
        );
    }

    private void createLog(
            AuditLogAction action,
            AuditActor actor,
            AuditLogTargetType targetType,
            UUID targetId,
            String targetName,
            String details
    ) {
        AuditLogEntity log = AuditLogEntity.builder()
                .action(action)
                .actorUserId(actor.userId())
                .actorUsername(actor.username())
                .actorEmail(actor.email())
                .targetType(targetType)
                .targetId(targetId)
                .targetName(targetName)
                .details(details)
                .build();

        auditLogRepository.save(log);
    }
}
