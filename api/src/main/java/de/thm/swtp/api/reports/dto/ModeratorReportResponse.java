package de.thm.swtp.api.reports.dto;

import de.thm.swtp.api.reports.domain.Report;
import de.thm.swtp.api.reports.domain.ReportReason;
import de.thm.swtp.api.reports.domain.ReportStatus;
import de.thm.swtp.api.reports.domain.ReportTarget;

import java.time.LocalDateTime;
import java.util.UUID;

/** Response DTO for reports shown in the moderation reports page.*/
public record ModeratorReportResponse(UUID id,
                                      UUID reporterId,
                                      String reporterUsername,
                                      ReportTarget target,
                                      UUID targetId,
                                      ReportReason reason,
                                      String message,
                                      ReportStatus status,
                                      UUID reviewerKeycloakId,
                                      String reviewerUsername,
                                      LocalDateTime reviewedAt,
                                      String moderatorMessage,
                                      LocalDateTime createdAt,
                                      LocalDateTime updatedAt) {

    /** Converts a report domain object into a response DTO for moderators.*/
    public static ModeratorReportResponse toResponse(Report report) {
        return new ModeratorReportResponse(
                report.getId(),
                report.getReporterId(),
                report.getReporterUsername(),
                report.getTarget(),
                report.getTargetId(),
                report.getReason(),
                report.getMessage(),
                report.getStatus(),
                report.getReviewerKeycloakId(),
                report.getReviewerUsername(),
                report.getReviewedAt(),
                report.getModeratorMessage(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
