package de.thm.swtp.api.reports.mapper;

import de.thm.swtp.api.reports.domain.Report;
import de.thm.swtp.api.reports.domain.ReportTargetSummary;
import de.thm.swtp.api.reports.entity.ReportEntity;

/** Mapper to convert a report-entity into a report domain object.*/
public class ReportMapper {

    public static Report toDomain(ReportEntity entity) {
        return toDomain(entity, null, 0);
    }

    public static Report toDomain(ReportEntity entity, ReportTargetSummary targetSummary) {
        return toDomain(entity, targetSummary, 0);
    }

    public static Report toDomain(ReportEntity entity, ReportTargetSummary targetSummary, long similarReportsCount) {
        return Report.builder()
                .id(entity.getId())
                .reporterId(entity.getReporter().getKeycloakId())
                .reporterUsername(entity.getReporter().getUsername())
                .target(entity.getTarget())
                .targetId(entity.getTargetId())
                .reason(entity.getReason())
                .message(entity.getMessage())
                .status(entity.getStatus())
                .targetSummary(targetSummary)
                .reviewerKeycloakId(entity.getReviewerKeycloakId())
                .reviewerUsername(entity.getReviewerUsername())
                .reviewedAt(entity.getReviewedAt())
                .moderatorMessage(entity.getModeratorMessage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .similarReportsCount(similarReportsCount)
                .build();
    }
}
