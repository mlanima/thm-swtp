package de.thm.swtp.api.auditlog;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public Page<AuditLogEntity> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
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
                "Project post was deleted from project \"" + projectName + "\" (" + projectId + ")."
        );
    }

    public void logUserBanned(AuditActor actor, UUID bannedUserId, String bannedUsername, String reason) {
        createLog(
                AuditLogAction.USER_BANNED,
                actor,
                AuditLogTargetType.USER,
                bannedUserId,
                bannedUsername,
                reason == null || reason.isBlank()
                        ? "User was banned without a reason."
                        : "User was banned. Reason: " + reason
        );
    }

    public void logUserUnbanned(AuditActor actor, UUID unbannedUserId, String unbannedUsername) {
        createLog(
                AuditLogAction.USER_UNBANNED,
                actor,
                AuditLogTargetType.USER,
                unbannedUserId,
                unbannedUsername,
                "User was unbanned."
        );
    }

    public void logProfessorRequestAccepted(AuditActor actor, UUID requestId, String requestingUsername, String requestingEmail) {
        createLog(
                AuditLogAction.PROFESSOR_REQUEST_ACCEPTED,
                actor,
                AuditLogTargetType.PROFESSOR_REQUEST,
                requestId,
                requestingUsername,
                "Professor request was accepted for " + requestingEmail + "."
        );
    }

    public void logProfessorRequestRejected(AuditActor actor, UUID requestId, String requestingUsername, String requestingEmail) {
        createLog(
                AuditLogAction.PROFESSOR_REQUEST_REJECTED,
                actor,
                AuditLogTargetType.PROFESSOR_REQUEST,
                requestId,
                requestingUsername,
                "Professor request was rejected for " + requestingEmail + "."
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

    private record ActorInfo(String username, String email) {
    }
}