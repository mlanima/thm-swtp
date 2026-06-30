package de.thm.swtp.api.professorRequest.controller;

import de.thm.swtp.api.professorRequest.domain.ProfessorRequest;
import de.thm.swtp.api.professorRequest.dto.CreateProfessorRequestRequest;
import de.thm.swtp.api.professorRequest.dto.ProfessorRequestResponse;
import de.thm.swtp.api.professorRequest.service.ProfessorRequestService;

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

import java.util.List;
import java.util.UUID;

/** REST controller for managing professor-rights requests. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/professor-requests")
public class ProfessorRequestController {
    private final ProfessorRequestService professorRequestService;

    /** Returns all professor-rights requests (paginated). */
    @GetMapping
    @PreAuthorize("@security.canViewAllProfessorRequests(authentication)")
    public Page<ProfessorRequestResponse> getAllProfessorRequests(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return professorRequestService.getAllProfessorRequests(pageable)
                .map(ProfessorRequestResponse::toResponse);
    }

    /** Returns all requests for a specific user. */
    @GetMapping("/{userId}")
    @PreAuthorize("@security.canViewProfessorRequestForUser(#userId, authentication)")
    public List<ProfessorRequestResponse> getRequestsByUser(@PathVariable UUID userId) {
        return professorRequestService.getRequestsByUser(userId)
                .stream()
                .map(ProfessorRequestResponse::toResponse)
                .toList();
    }

    /** Creates a new professor-rights request for the authenticated user with status PENDING. */
    @PostMapping
    @PreAuthorize("@security.canCreateProfessorRequest(authentication)")
    public ProfessorRequestResponse createProfessorRequest(
            @Valid @RequestBody CreateProfessorRequestRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        ProfessorRequest professorRequest = professorRequestService.createProfessorRequest(
                currentUserId, request.email(), request.text());

        return ProfessorRequestResponse.toResponse(professorRequest);
    }

    /** Accepts a professor-rights request by setting its status to ACCEPTED. */
    @PatchMapping("/{requestId}/accept")
    @PreAuthorize("@security.canManageProfessorRequests(authentication)")
    public ProfessorRequestResponse acceptProfessorRequest(@PathVariable UUID requestId) {
        ProfessorRequest professorRequest = professorRequestService.acceptProfessorRequest(requestId);
        return ProfessorRequestResponse.toResponse(professorRequest);
    }

    /** Rejects a professor-rights request by setting its status to REJECTED. */
    @PatchMapping("/{requestId}/reject")
    @PreAuthorize("@security.canManageProfessorRequests(authentication)")
    public ProfessorRequestResponse rejectProfessorRequest(@PathVariable UUID requestId) {
        ProfessorRequest professorRequest = professorRequestService.rejectProfessorRequest(requestId);
        return ProfessorRequestResponse.toResponse(professorRequest);
    }
}
