package de.thm.swtp.api.reports.dto;

import de.thm.swtp.api.reports.domain.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateReportStatusRequest(@NotNull ReportStatus reportStatus, @Size(max = 1000) String moderatorMessage) {
}
