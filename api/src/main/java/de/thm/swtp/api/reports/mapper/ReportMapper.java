package de.thm.swtp.api.reports.mapper;

import de.thm.swtp.api.reports.domain.Report;
import de.thm.swtp.api.reports.entity.ReportEntity;

public class ReportMapper {

    public static Report toDomain(ReportEntity reportEntity) {
        return Report.builder()
                .id(reportEntity.getId())
                .reporterId(reportEntity.getReporter().getKeycloakId())
                .reporterUsername(reportEntity.getReporter().getUsername())
                .target(reportEntity.getTarget())
                .reason(reportEntity.getReason())
                .message(reportEntity.getMessage())
                .status(reportEntity.getStatus())
                .reviewerKeycloakId(reportEntity.getReviewerKeycloakId())
                .reviewerUsername(reportEntity.getReviewerUsername())
                .reviewedAt(reportEntity.getReviewedAt())
                .moderatorMessage(reportEntity.getModeratorMessage())
                .createdAt(reportEntity.getCreatedAt())
                .updatedAt(reportEntity.getUpdatedAt())
                .build();
    }
}
