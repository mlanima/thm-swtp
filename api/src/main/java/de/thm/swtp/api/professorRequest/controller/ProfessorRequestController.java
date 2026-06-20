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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** REST controller for managing professor-rights requests. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/professor-requests")
public class ProfessorRequestController {
    private final ProfessorRequestService professorRequestService;

    /** Returns all professor-rights requests (paginated).
     *  TODO: Restrict to admin users only.
     */
    @GetMapping
    public Page<ProfessorRequestResponse> getAllProfessorRequests(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return professorRequestService.getAllProfessorRequests(pageable)
                .map(ProfessorRequestResponse::toResponse);
    }

    /** Creates a new professor-rights request for the authenticated user with status PENDING. */
    @PostMapping
    public ProfessorRequestResponse createProfessorRequest(
            @Valid @RequestBody CreateProfessorRequestRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        ProfessorRequest professorRequest = professorRequestService.createProfessorRequest(
                currentUserId, request.name(), request.email(), request.text());

        return ProfessorRequestResponse.toResponse(professorRequest);
    }

    /** Accepts a professor-rights request by setting its status to ACCEPTED.
     *  TODO: Restrict to admin users only.
     */
    @PatchMapping("/{requestId}/accept")
    public ProfessorRequestResponse acceptProfessorRequest(@PathVariable UUID requestId) {
        ProfessorRequest professorRequest = professorRequestService.acceptProfessorRequest(requestId);
        return ProfessorRequestResponse.toResponse(professorRequest);
    }

    /** Rejects a professor-rights request by setting its status to REJECTED.
     *  TODO: Restrict to admin users only.
     */
    @PatchMapping("/{requestId}/reject")
    public ProfessorRequestResponse rejectProfessorRequest(@PathVariable UUID requestId) {
        ProfessorRequest professorRequest = professorRequestService.rejectProfessorRequest(requestId);
        return ProfessorRequestResponse.toResponse(professorRequest);
    }
}
