package de.thm.swtp.api.auditlog.service;

import de.thm.swtp.api.auditlog.domain.AuditActor;
import de.thm.swtp.api.auditlog.domain.AuditLogAction;
import de.thm.swtp.api.auditlog.domain.AuditLogTargetType;
import de.thm.swtp.api.auditlog.entity.AuditLogEntity;
import de.thm.swtp.api.auditlog.repository.AuditLogRepository;
import de.thm.swtp.api.exceptionhandling.exceptions.InvalidAuditLogSortFieldException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AuditLogServiceTest {

    private AuditLogRepository auditLogRepository;
    private AuditLogService auditLogService;
    private AuditActor actor;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        auditLogService = new AuditLogService(auditLogRepository);

        actor = new AuditActor(
                UUID.randomUUID(),
                "moderator",
                "moderator@test.de"
        );
    }

    @Test
    void getAuditLogs_shouldAcceptAllowedSortField() {
        PageRequest pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        auditLogService.getAuditLogs(pageable);

        verify(auditLogRepository).findAll(pageable);
    }

    @Test
    void getAuditLogs_shouldThrow_whenSortFieldIsInvalid() {
        PageRequest pageable = PageRequest.of(
                0,
                20,
                Sort.by("doesNotExist")
        );

        assertThatThrownBy(() -> auditLogService.getAuditLogs(pageable))
                .isInstanceOf(InvalidAuditLogSortFieldException.class)
                .hasMessageContaining("doesNotExist");

        verify(auditLogRepository, never()).findAll(pageable);
    }

    @Test
    void logUserBanned_shouldStoreOnlyReasonAsDetails() {
        UUID userId = UUID.randomUUID();

        auditLogService.logUserBanned(actor, userId, "bob", "Spam");

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntity savedLog = captor.getValue();

        assertThat(savedLog.getAction()).isEqualTo(AuditLogAction.USER_BANNED);
        assertThat(savedLog.getActorUserId()).isEqualTo(actor.userId());
        assertThat(savedLog.getActorUsername()).isEqualTo("moderator");
        assertThat(savedLog.getActorEmail()).isEqualTo("moderator@test.de");
        assertThat(savedLog.getTargetType()).isEqualTo(AuditLogTargetType.USER);
        assertThat(savedLog.getTargetId()).isEqualTo(userId);
        assertThat(savedLog.getTargetName()).isEqualTo("bob");
        assertThat(savedLog.getDetails()).isEqualTo("Spam");
    }

    @Test
    void logUserBanned_shouldStoreFallback_whenReasonIsBlank() {
        UUID userId = UUID.randomUUID();

        auditLogService.logUserBanned(actor, userId, "bob", " ");

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        assertThat(captor.getValue().getDetails()).isEqualTo("—");
    }

    @Test
    void logProjectPostDeleted_shouldStoreProjectInfoAsDetails() {
        UUID postId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        auditLogService.logProjectPostDeleted(
                actor,
                postId,
                "Sprint Update",
                projectId,
                "IdeaCamp"
        );

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntity savedLog = captor.getValue();

        assertThat(savedLog.getAction()).isEqualTo(AuditLogAction.PROJECT_POST_DELETED);
        assertThat(savedLog.getTargetType()).isEqualTo(AuditLogTargetType.PROJECT_POST);
        assertThat(savedLog.getTargetId()).isEqualTo(postId);
        assertThat(savedLog.getTargetName()).isEqualTo("Sprint Update");
        assertThat(savedLog.getDetails()).isEqualTo("IdeaCamp (" + projectId + ")");
    }

    @Test
    void logProfessorRequestAccepted_shouldStoreEmailAsDetails() {
        UUID requestId = UUID.randomUUID();

        auditLogService.logProfessorRequestAccepted(
                actor,
                requestId,
                "profUser",
                "prof@thm.de"
        );

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntity savedLog = captor.getValue();

        assertThat(savedLog.getAction()).isEqualTo(AuditLogAction.PROFESSOR_REQUEST_ACCEPTED);
        assertThat(savedLog.getTargetType()).isEqualTo(AuditLogTargetType.PROFESSOR_REQUEST);
        assertThat(savedLog.getTargetName()).isEqualTo("profUser");
        assertThat(savedLog.getDetails()).isEqualTo("prof@thm.de");
    }
}