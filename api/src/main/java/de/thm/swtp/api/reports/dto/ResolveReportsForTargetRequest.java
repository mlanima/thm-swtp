package de.thm.swtp.api.reports.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for resolving all active reports that belong to the same reported target.*/
public record ResolveReportsForTargetRequest(@NotBlank String moderatorMessage) {
}
