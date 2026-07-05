package de.thm.swtp.api.reports.dto;

import de.thm.swtp.api.reports.domain.ReportReason;
import de.thm.swtp.api.reports.domain.ReportTarget;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Request DTO for creating a report.*/
public record CreateReportRequest(@NotNull ReportTarget target, @NotNull UUID targetId, @NotNull ReportReason reason, @Size(max = 1000) String message) {
}
