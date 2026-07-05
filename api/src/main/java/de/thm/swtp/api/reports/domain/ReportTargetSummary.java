package de.thm.swtp.api.reports.domain;

import java.util.UUID;

/** Displays summary of the reported target.*/
public record ReportTargetSummary(String title, String subtitle, String link, UUID parentId) {
}
