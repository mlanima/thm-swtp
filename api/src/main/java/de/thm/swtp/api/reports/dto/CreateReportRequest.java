package de.thm.swtp.api.reports.dto;

import de.thm.swtp.api.reports.domain.ReportReason;
import de.thm.swtp.api.reports.domain.ReportTarget;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateReportRequest(@NotNull ReportTarget reportTarget, @NotNull UUID targetId, @NotNull ReportReason reportReason, @Size(max = 1000) String message) {
}
