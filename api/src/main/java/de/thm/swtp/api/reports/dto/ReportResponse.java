package de.thm.swtp.api.reports.dto;

import de.thm.swtp.api.reports.domain.Report;
import de.thm.swtp.api.reports.domain.ReportReason;
import de.thm.swtp.api.reports.domain.ReportStatus;
import de.thm.swtp.api.reports.domain.ReportTarget;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReportResponse(UUID id,
                             UUID reporterId,
                             String reporterUsername,
                             ReportTarget target,
                             UUID targetId,
                             ReportReason reason,
                             String message,
                             ReportStatus status,
                             UUID reviewedByKeycloakId,
                             String reviewedByUsername,
                             LocalDateTime reviewedAt,
                             String moderatorNote,
                             LocalDateTime createdAt,
                             LocalDateTime updatedAt
) {

    /**
     * Converts a report domain object into a response DTO.
     */
    public static ReportResponse toResponse(Report report) {
        return new ReportResponse(
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
