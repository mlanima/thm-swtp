package de.thm.swtp.api.reports.controller;

import de.thm.swtp.api.common.PageResponse;
import de.thm.swtp.api.reports.domain.ReportReason;
import de.thm.swtp.api.reports.domain.ReportStatus;
import de.thm.swtp.api.reports.domain.ReportTarget;
import de.thm.swtp.api.reports.dto.CreateReportRequest;
import de.thm.swtp.api.reports.dto.ModeratorReportResponse;
import de.thm.swtp.api.reports.dto.ReportResponse;
import de.thm.swtp.api.reports.dto.UpdateReportStatusRequest;
import de.thm.swtp.api.reports.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** REST Controller for creating and managing reports submitted by users.*/
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/reports")
public class ReportController {
    private final ReportService reportService;


    /** Returns reports using filters, search and sorting. For moderation purpose.*/
    @GetMapping
    @PreAuthorize("@security.canViewReports(authentication)")
    public PageResponse<ModeratorReportResponse> getReports(@RequestParam(required = false) ReportStatus status,
                                                            @RequestParam(required = false) ReportTarget target,
                                                            @RequestParam(required = false) ReportReason reason,
                                                            @RequestParam(required = false) String query,
                                                            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ModeratorReportResponse> reports = reportService.getReports(status, target, reason, query, pageable)
                .map(ModeratorReportResponse::toResponse);

        return PageResponse.toResponse(reports);
    }

    /** Creates a report for a user, project or project post.*/
    @PostMapping
    @PreAuthorize("@security.canCreateReport(authentication)")
    public ReportResponse createReport(@Valid @RequestBody CreateReportRequest createReportRequest, @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        return ReportResponse.toResponse(reportService.createReport(
                currentUserId,
                createReportRequest.reportTarget(),
                createReportRequest.targetId(),
                createReportRequest.reportReason(),
                createReportRequest.message()));
    }

    /** Updates the moderation status of a report.*/
    @PatchMapping("/{reportId}/status")
    @PreAuthorize("@security.canManageReports(authentication)")
    public ModeratorReportResponse updateReportStatus(@PathVariable UUID reportId, @Valid @RequestBody UpdateReportStatusRequest request, @AuthenticationPrincipal Jwt jwt) {
        UUID moderatorKeycloakId = UUID.fromString(jwt.getSubject());
        String moderatorUsername = jwt.getClaimAsString("preferred_username");

        return ModeratorReportResponse.toResponse(reportService.updateReportStatus(
                reportId,
                request.status(),
                moderatorKeycloakId,
                moderatorUsername,
                request.moderatorMessage()
        ));
    }

}
