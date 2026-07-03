package de.thm.swtp.api.auditlog;

import de.thm.swtp.api.auditlog.dto.AuditLogResponse;
import de.thm.swtp.api.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("@security.hasModeratorRole(authentication)")
    public PageResponse<AuditLogResponse> getAuditLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AuditLogResponse> logs = auditLogService.getAuditLogs(pageable)
                .map(AuditLogResponse::toResponse);

        return PageResponse.toResponse(logs);
    }
}