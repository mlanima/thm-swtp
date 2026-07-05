package de.thm.swtp.api.reports.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/** Domain model representing a report from a user. Reports are reviewed by moderators.
 * A report can target different types, see {@link ReportTarget}. The target is identified
 * by the combination of {@link ReportTarget} and {@code targetId}
 */
@Builder
@Value
public class Report {
    UUID id;
    UUID reporterId;
    String reporterUsername;
    ReportTarget target;
    UUID targetId;
    ReportReason reason;
    String message;
    ReportStatus status;
    ReportTargetSummary targetSummary;
    UUID reviewerKeycloakId;
    String reviewerUsername;
    LocalDateTime reviewedAt;
    String moderatorMessage;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    long similarReportsCount;
}
