package de.thm.swtp.api.reports.controller;

import de.thm.swtp.api.common.PageResponse;
import de.thm.swtp.api.reports.dto.ReportResponse;
import de.thm.swtp.api.reports.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/reports")
public class ReportController {
    private final ReportService reportService;


    @GetMapping
    @PreAuthorize("@security.canViewReports(authentication)")
    public PageResponse<ReportResponse> getReports(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ReportResponse> reports = reportService.getReports(pageable)
                .map(ReportResponse::toResponse);

        return PageResponse.toResponse(reports);
    }
}
