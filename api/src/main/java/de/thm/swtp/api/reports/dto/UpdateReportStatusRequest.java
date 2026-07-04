package de.thm.swtp.api.reports.dto;

import de.thm.swtp.api.reports.domain.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request DTO for updating the moderation status of a report.*/
public record UpdateReportStatusRequest(@NotNull ReportStatus reportStatus, @Size(max = 1000) String moderatorMessage) {
}
