package de.thm.swtp.api.reports.service;

import de.thm.swtp.api.exceptionhandling.exceptions.*;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.projectPost.entity.ProjectPostEntity;
import de.thm.swtp.api.projectPost.repository.ProjectPostRepository;
import de.thm.swtp.api.reports.domain.*;
import de.thm.swtp.api.reports.entity.ReportEntity;
import de.thm.swtp.api.reports.mapper.ReportMapper;
import de.thm.swtp.api.reports.repository.ReportRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/** Service for creating, searching and moderation reports.*/
@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProjectRepository projectRepository;
    private final ProjectPostRepository projectPostRepository;

    private static final Set<String> ALLOWED_REPORT_SORT_FIELDS = Set.of("target", "targetId", "reason", "status", "reporter.username",
                                                                         "reviewerUsername", "createdAt", "updatedAt", "reviewedAt");


    /** Creates a new report for a user, project or project post.*/
    @Transactional
    public Report createReport(UUID currentUserId, ReportTarget target, UUID targetId, ReportReason reason, String message) {
        UserProfile reporter = userProfileRepository.findById(currentUserId)
                .orElseThrow(() -> new UserProfileNotFoundException(currentUserId.toString()));

        if (target == ReportTarget.USER && targetId.equals(currentUserId)) {
            throw new InvalidReportTargetException("Users cannot report themselves.");
        }

        validateReportTarget(target,targetId);

        ReportEntity reportEntity = ReportEntity.builder()
                .reporter(reporter)
                .target(target)
                .targetId(targetId)
                .reason(reason)
                .message(message)
                .build();

        ReportEntity saved = reportRepository.save(reportEntity);
        return ReportMapper.toDomain(saved);

    }

    /** Returns reports for moderator review. Reports can be filtered, sorted or searched through a query. */
    @Transactional
    public Page<Report> getReports(ReportStatus status, ReportTarget target, ReportReason reason, String query, Pageable pageable) {
        validateReportSort(pageable);
       String normalizedQuery = normalizeQuery(query);
       return reportRepository.searchReports(status, target, reason, normalizedQuery, pageable)
               .map(this::toDomainWithTargetSummary);
    }

    /** Updates the moderation status of a report.*/
    @Transactional
    public Report updateReportStatus(UUID reportId, ReportStatus status, UUID moderatorKeycloakId, String moderatorUsername, String moderatorMessage) {
        ReportEntity reportEntity = getReport(reportId);
        canUpdateReportStatus(reportEntity);

        switch(status) {
            case IN_REVIEW -> markReportAsInReview(reportEntity, moderatorKeycloakId, moderatorUsername);
            case RESOLVED, DISMISSED -> closeReport(reportEntity, status, moderatorKeycloakId, moderatorUsername, moderatorMessage);
            case OPEN -> throw new  InvalidReportStatusException("Reports cannot be set back to open.");
        }

        ReportEntity saved =  reportRepository.save(reportEntity);
        return toDomainWithTargetSummary(saved);
    }


    /** Checks if the report target exists.*/
    private void validateReportTarget(ReportTarget target, UUID targetId){
        boolean exists = switch (target){
            case USER -> userProfileRepository.existsById(targetId);
            case PROJECT -> projectRepository.existsById(targetId);
            case PROJECT_POST -> projectPostRepository.existsById(targetId);
        };

        if (!exists){
            throw new ReportTargetNotFoundException("Reported target not found:" + targetId);
        }
    }

    /** Checks if the requested sort parameter is supported.*/
    private void validateReportSort(Pageable pageable){
        pageable.getSort().forEach(sortField -> {
            if (!ALLOWED_REPORT_SORT_FIELDS.contains(sortField.getProperty())) {
                throw new InvalidReportSortFieldException("Unsupported sort field: " + sortField.getProperty());
            }
        });
    }

    /** Converts a report entity into a domain model with the target display information.*/
    private Report toDomainWithTargetSummary(ReportEntity reportEntity) {
        ReportTargetSummary targetSummary = buildTargetSummary(reportEntity.getTarget(), reportEntity.getTargetId());
        return ReportMapper.toDomain(reportEntity, targetSummary);
    }

    private ReportTargetSummary buildTargetSummary(ReportTarget target, UUID targetId){
        return switch (target) {
            case USER -> buildUserTargetSummary(targetId);
            case PROJECT -> buildProjectTargetSummary(targetId);
            case PROJECT_POST -> buildProjectPostTargetSummary(targetId);
        };
    }

    /** Builds display information for a reported user profile. */
    private ReportTargetSummary buildUserTargetSummary(UUID userId) {
        return userProfileRepository.findById(userId)
                .map(user -> new ReportTargetSummary(
                        user.getUsername(),
                        "USER",
                        "/profiles/" + user.getUsername()
                ))
                .orElse(new ReportTargetSummary(
                        "Deleted user",
                        "USER",
                        null
                ));
    }

    /** Builds display information for a reported project. */
    private ReportTargetSummary buildProjectTargetSummary(UUID projectId) {
        return projectRepository.findById(projectId)
                .map(project -> new ReportTargetSummary(
                        project.getName(),
                        "PROJECT",
                        "/project/" + project.getProjectUrl()
                ))
                .orElse(new ReportTargetSummary(
                        "Deleted project",
                        "PROJECT",
                        null
                ));
    }

    /** Builds display information for a reported project post. */
    private ReportTargetSummary buildProjectPostTargetSummary(UUID postId) {
        return projectPostRepository.findById(postId)
                .map(post -> new ReportTargetSummary(
                        getProjectPostTitle(post),
                        post.getProject().getName(),
                        "/project/" + post.getProject().getProjectUrl()
                ))
                .orElse(new ReportTargetSummary(
                        "Deleted project post",
                        "PROJECT_POST",
                        null
                ));
    }

    /** Returns a short display title for a project post. */
    private String getProjectPostTitle(ProjectPostEntity post) {
        if (post.getTitle() != null && !post.getTitle().isBlank()) {
            return post.getTitle();
        }

        if (post.getContent() == null || post.getContent().isBlank()) {
            return "Project post";
        }

        return post.getContent().length() <= 80
                ? post.getContent()
                : post.getContent().substring(0, 80) + "...";
    }

    private ReportEntity getReport(UUID reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found: " + reportId));
    }

    /** Ensures that only open or in-review reports can be updated.*/
    private void canUpdateReportStatus(ReportEntity reportEntity){
        if (reportEntity.getStatus() == ReportStatus.RESOLVED || reportEntity.getStatus() == ReportStatus.DISMISSED) {
            throw new InvalidReportStatusException("Only open or in-review reports can be updated.");
        }
    }

    private void markReportAsInReview(ReportEntity reportEntity, UUID moderatorKeycloakId, String moderatorUsername) {
        reportEntity.setStatus(ReportStatus.IN_REVIEW);
        reportEntity.setReviewerKeycloakId(moderatorKeycloakId);
        reportEntity.setReviewerUsername(moderatorUsername);
        reportEntity.setReviewedAt(LocalDateTime.now());
    }

    private void closeReport(ReportEntity reportEntity, ReportStatus status, UUID moderatorKeycloakId, String moderatorUsername, String moderatorMessage) {
        reportEntity.setStatus(status);
        reportEntity.setReviewerKeycloakId(moderatorKeycloakId);
        reportEntity.setReviewerUsername(moderatorUsername);
        reportEntity.setReviewedAt(LocalDateTime.now());
        reportEntity.setModeratorMessage(moderatorMessage);
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        return "%" +  query.trim().toLowerCase() + "%";
    }

}


