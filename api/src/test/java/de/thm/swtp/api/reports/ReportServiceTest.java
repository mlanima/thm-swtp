package de.thm.swtp.api.reports;

import de.thm.swtp.api.exceptionhandling.exceptions.InvalidReportStatusException;
import de.thm.swtp.api.exceptionhandling.exceptions.ReportAlreadyExistsException;
import de.thm.swtp.api.exceptionhandling.exceptions.ReportNotFoundException;
import de.thm.swtp.api.exceptionhandling.exceptions.ReportTargetNotFoundException;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.projectPost.repository.ProjectPostRepository;
import de.thm.swtp.api.reports.domain.Report;
import de.thm.swtp.api.reports.domain.ReportReason;
import de.thm.swtp.api.reports.domain.ReportStatus;
import de.thm.swtp.api.reports.domain.ReportTarget;
import de.thm.swtp.api.reports.entity.ReportEntity;
import de.thm.swtp.api.reports.repository.ReportRepository;
import de.thm.swtp.api.reports.service.ReportService;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectPostRepository projectPostRepository;

    private ReportService reportService;

    private UUID reporterId;
    private UUID targetId;
    private UUID reportId;
    private UUID moderatorId;

    private UserProfile reporter;
    private ReportEntity reportEntity;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                reportRepository,
                userProfileRepository,
                projectRepository,
                projectPostRepository
        );

        reporterId = UUID.randomUUID();
        targetId = UUID.randomUUID();
        reportId = UUID.randomUUID();
        moderatorId = UUID.randomUUID();

        reporter = UserProfile.builder()
                .keycloakId(reporterId)
                .username("reporter")
                .email("reporter@test.de")
                .build();

        reportEntity = ReportEntity.builder()
                .id(reportId)
                .reporter(reporter)
                .target(ReportTarget.PROJECT)
                .targetId(targetId)
                .reason(ReportReason.SPAM)
                .message("This looks like spam.")
                .status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createReport_shouldCreateReport_whenTargetIsUser() {
        when(userProfileRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(userProfileRepository.existsById(targetId)).thenReturn(true);
        when(reportRepository.save(any(ReportEntity.class)))
                .thenAnswer(invocation -> {
                    ReportEntity saved = invocation.getArgument(0);
                    saved.setId(reportId);
                    saved.setCreatedAt(LocalDateTime.now());
                    saved.setUpdatedAt(LocalDateTime.now());
                    return saved;
                });

        Report result = reportService.createReport(
                reporterId,
                ReportTarget.USER,
                targetId,
                ReportReason.HARASSMENT,
                "User is insulting others."
        );

        assertThat(result.getId()).isEqualTo(reportId);
        assertThat(result.getReporterId()).isEqualTo(reporterId);
        assertThat(result.getReporterUsername()).isEqualTo("reporter");
        assertThat(result.getTarget()).isEqualTo(ReportTarget.USER);
        assertThat(result.getTargetId()).isEqualTo(targetId);
        assertThat(result.getReason()).isEqualTo(ReportReason.HARASSMENT);
        assertThat(result.getMessage()).isEqualTo("User is insulting others.");
        assertThat(result.getStatus()).isEqualTo(ReportStatus.OPEN);

        verify(userProfileRepository).existsById(targetId);
        verify(reportRepository).save(any(ReportEntity.class));
    }

    @Test
    void createReport_shouldCreateReport_whenTargetIsProject() {
        when(userProfileRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(projectRepository.existsById(targetId)).thenReturn(true);
        when(reportRepository.save(any(ReportEntity.class)))
                .thenAnswer(invocation -> {
                    ReportEntity saved = invocation.getArgument(0);
                    saved.setId(reportId);
                    saved.setCreatedAt(LocalDateTime.now());
                    saved.setUpdatedAt(LocalDateTime.now());
                    return saved;
                });

        Report result = reportService.createReport(
                reporterId,
                ReportTarget.PROJECT,
                targetId,
                ReportReason.SPAM,
                "Project looks suspicious."
        );

        assertThat(result.getTarget()).isEqualTo(ReportTarget.PROJECT);
        assertThat(result.getTargetId()).isEqualTo(targetId);
        assertThat(result.getReason()).isEqualTo(ReportReason.SPAM);
        assertThat(result.getMessage()).isEqualTo("Project looks suspicious.");
        assertThat(result.getStatus()).isEqualTo(ReportStatus.OPEN);

        verify(projectRepository).existsById(targetId);
        verify(reportRepository).save(any(ReportEntity.class));
    }

    @Test
    void createReport_shouldCreateReport_whenTargetIsProjectPost() {
        reportEntity.setTarget(ReportTarget.PROJECT_POST);

        when(userProfileRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(projectPostRepository.existsById(targetId)).thenReturn(true);
        when(reportRepository.save(any(ReportEntity.class)))
                .thenAnswer(invocation -> {
                    ReportEntity saved = invocation.getArgument(0);
                    saved.setId(reportId);
                    saved.setCreatedAt(LocalDateTime.now());
                    saved.setUpdatedAt(LocalDateTime.now());
                    return saved;
                });

        Report result = reportService.createReport(
                reporterId,
                ReportTarget.PROJECT_POST,
                targetId,
                ReportReason.HATE_SPEECH,
                "Post contains hate speech."
        );

        assertThat(result.getTarget()).isEqualTo(ReportTarget.PROJECT_POST);
        assertThat(result.getReason()).isEqualTo(ReportReason.HATE_SPEECH);

        verify(projectPostRepository).existsById(targetId);
        verify(reportRepository).save(any(ReportEntity.class));
    }

    @Test
    void createReport_shouldThrow_whenReporterDoesNotExist() {
        when(userProfileRepository.findById(reporterId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.createReport(
                reporterId,
                ReportTarget.PROJECT,
                targetId,
                ReportReason.SPAM,
                "Message"
        ))
                .isInstanceOf(UserProfileNotFoundException.class);

        verify(reportRepository, never()).save(any());
        verify(projectRepository, never()).existsById(any());
    }

    @Test
    void createReport_shouldThrow_whenTargetDoesNotExist() {
        when(userProfileRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(projectRepository.existsById(targetId)).thenReturn(false);

        assertThatThrownBy(() -> reportService.createReport(
                reporterId,
                ReportTarget.PROJECT,
                targetId,
                ReportReason.SPAM,
                "Message"
        ))
                .isInstanceOf(ReportTargetNotFoundException.class);

        verify(reportRepository, never()).save(any());
    }

    @Test
    void getReports_shouldReturnPagedReports() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReportEntity> page = new PageImpl<>(List.of(reportEntity), pageable, 1);

        when(reportRepository.searchReports(
                ReportStatus.OPEN,
                ReportTarget.PROJECT,
                ReportReason.SPAM,
                "%spam%",
                pageable
        )).thenReturn(page);

        Page<Report> result = reportService.getReports(
                ReportStatus.OPEN,
                ReportTarget.PROJECT,
                ReportReason.SPAM,
                "spam",
                pageable
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(reportId);
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(reportRepository).searchReports(
                ReportStatus.OPEN,
                ReportTarget.PROJECT,
                ReportReason.SPAM,
                "%spam%",
                pageable
        );
    }

    @Test
    void updateReportStatus_shouldSetStatusToInReview() {
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(reportEntity));
        when(reportRepository.save(reportEntity)).thenReturn(reportEntity);

        Report result = reportService.updateReportStatus(
                reportId,
                ReportStatus.IN_REVIEW,
                moderatorId,
                "moderator",
                null
        );

        assertThat(result.getStatus()).isEqualTo(ReportStatus.IN_REVIEW);
        assertThat(reportEntity.getReviewerKeycloakId()).isEqualTo(moderatorId);
        assertThat(reportEntity.getReviewerUsername()).isEqualTo("moderator");
        assertThat(reportEntity.getReviewedAt()).isNotNull();
        assertThat(reportEntity.getModeratorMessage()).isNull();

        verify(reportRepository).save(reportEntity);
    }

    @Test
    void updateReportStatus_shouldResolveReportWithModeratorMessage() {
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(reportEntity));
        when(reportRepository.save(reportEntity)).thenReturn(reportEntity);

        Report result = reportService.updateReportStatus(
                reportId,
                ReportStatus.RESOLVED,
                moderatorId,
                "moderator",
                "Handled by moderator."
        );

        assertThat(result.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(reportEntity.getModeratorMessage()).isEqualTo("Handled by moderator.");
        assertThat(reportEntity.getReviewerKeycloakId()).isEqualTo(moderatorId);
        assertThat(reportEntity.getReviewedAt()).isNotNull();

        verify(reportRepository).save(reportEntity);
    }

    @Test
    void updateReportStatus_shouldDismissReportWithModeratorMessage() {
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(reportEntity));
        when(reportRepository.save(reportEntity)).thenReturn(reportEntity);

        Report result = reportService.updateReportStatus(
                reportId,
                ReportStatus.DISMISSED,
                moderatorId,
                "moderator",
                "No violation found."
        );

        assertThat(result.getStatus()).isEqualTo(ReportStatus.DISMISSED);
        assertThat(reportEntity.getModeratorMessage()).isEqualTo("No violation found.");
        assertThat(reportEntity.getReviewerUsername()).isEqualTo("moderator");

        verify(reportRepository).save(reportEntity);
    }

    @Test
    void updateReportStatus_shouldThrow_whenReportDoesNotExist() {
        when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.updateReportStatus(
                reportId,
                ReportStatus.IN_REVIEW,
                moderatorId,
                "moderator",
                null
        ))
                .isInstanceOf(ReportNotFoundException.class);

        verify(reportRepository, never()).save(any());
    }

    @Test
    void updateReportStatus_shouldThrow_whenStatusIsOpen() {
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(reportEntity));

        assertThatThrownBy(() -> reportService.updateReportStatus(
                reportId,
                ReportStatus.OPEN,
                moderatorId,
                "moderator",
                null
        ))
                .isInstanceOf(InvalidReportStatusException.class);

        verify(reportRepository, never()).save(any());
    }

    @Test
    void updateReportStatus_shouldThrow_whenReportIsAlreadyResolved() {
        reportEntity.setStatus(ReportStatus.RESOLVED);

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(reportEntity));

        assertThatThrownBy(() -> reportService.updateReportStatus(
                reportId,
                ReportStatus.DISMISSED,
                moderatorId,
                "moderator",
                "Trying to change closed report."
        ))
                .isInstanceOf(InvalidReportStatusException.class);

        verify(reportRepository, never()).save(any());
    }

    @Test
    void createReport_shouldThrow_whenActiveReportForSameTargetAndReasonAlreadyExists() {
        when(userProfileRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(reportRepository.existsByReporterKeycloakIdAndTargetAndTargetIdAndReasonAndStatusIn(
                reporterId,
                ReportTarget.PROJECT,
                targetId,
                ReportReason.SPAM,
                List.of(ReportStatus.OPEN, ReportStatus.IN_REVIEW)
        )).thenReturn(true);

        assertThatThrownBy(() -> reportService.createReport(
                reporterId,
                ReportTarget.PROJECT,
                targetId,
                ReportReason.SPAM,
                "Spam report"
        )).isInstanceOf(ReportAlreadyExistsException.class);

        verify(projectRepository, never()).existsById(any());
        verify(reportRepository, never()).save(any());
    }

    @Test
    void createReport_shouldCreateReport_whenSameTargetWasReportedWithDifferentReason() {
        when(userProfileRepository.findById(reporterId)).thenReturn(Optional.of(reporter));

        when(reportRepository.existsByReporterKeycloakIdAndTargetAndTargetIdAndReasonAndStatusIn(
                eq(reporterId),
                eq(ReportTarget.PROJECT),
                eq(targetId),
                eq(ReportReason.HATE_SPEECH),
                anyCollection()
        )).thenReturn(false);

        when(projectRepository.existsById(targetId)).thenReturn(true);

        when(reportRepository.save(any(ReportEntity.class))).thenAnswer(invocation -> {
            ReportEntity saved = invocation.getArgument(0);
            saved.setId(reportId);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        Report result = reportService.createReport(
                reporterId,
                ReportTarget.PROJECT,
                targetId,
                ReportReason.HATE_SPEECH,
                "Contains hate speech"
        );

        assertThat(result.getReason()).isEqualTo(ReportReason.HATE_SPEECH);
        assertThat(result.getTarget()).isEqualTo(ReportTarget.PROJECT);
        assertThat(result.getTargetId()).isEqualTo(targetId);

        verify(projectRepository).existsById(targetId);
        verify(reportRepository).save(any(ReportEntity.class));
    }

    @Test
    void resolveActiveReportsForTarget_shouldResolveOpenAndInReviewReports() {
        ReportEntity openReport = ReportEntity.builder()
                .id(UUID.randomUUID())
                .target(ReportTarget.PROJECT)
                .targetId(targetId)
                .status(ReportStatus.OPEN)
                .build();

        ReportEntity inReviewReport = ReportEntity.builder()
                .id(UUID.randomUUID())
                .target(ReportTarget.PROJECT)
                .targetId(targetId)
                .status(ReportStatus.IN_REVIEW)
                .build();

        UUID moderatorId = UUID.randomUUID();

        when(reportRepository.findAllByTargetAndTargetIdAndStatusIn(
                eq(ReportTarget.PROJECT),
                eq(targetId),
                anyCollection()
        )).thenReturn(List.of(openReport, inReviewReport));

        reportService.resolveActiveReportsForTarget(
                ReportTarget.PROJECT,
                targetId,
                moderatorId,
                "moderator",
                "Project was deleted after report review."
        );

        assertThat(openReport.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(inReviewReport.getStatus()).isEqualTo(ReportStatus.RESOLVED);

        assertThat(openReport.getReviewerKeycloakId()).isEqualTo(moderatorId);
        assertThat(inReviewReport.getReviewerKeycloakId()).isEqualTo(moderatorId);

        assertThat(openReport.getReviewerUsername()).isEqualTo("moderator");
        assertThat(inReviewReport.getReviewerUsername()).isEqualTo("moderator");

        assertThat(openReport.getReviewedAt()).isNotNull();
        assertThat(inReviewReport.getReviewedAt()).isNotNull();

        assertThat(openReport.getModeratorMessage()).isEqualTo("Project was deleted after report review.");
        assertThat(inReviewReport.getModeratorMessage()).isEqualTo("Project was deleted after report review.");
    }
}
