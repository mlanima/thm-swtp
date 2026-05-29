package de.thm.swtp.api.projectJoinRequest.controller;

import de.thm.swtp.api.projectJoinRequest.domain.ProjectJoinRequest;
import de.thm.swtp.api.projectJoinRequest.dto.CreateProjectJoinRequestRequest;
import de.thm.swtp.api.projectJoinRequest.dto.ProjectJoinRequestResponse;
import de.thm.swtp.api.projectJoinRequest.service.ProjectJoinRequestService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/join-requests")
public class ProjectJoinRequestController {
    private final ProjectJoinRequestService projectJoinRequestService;

    @PostMapping
    public ProjectJoinRequestResponse createProjectJoinRequest(@PathVariable UUID projectId,
                                                               @Valid @RequestBody CreateProjectJoinRequestRequest request,
                                                               @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getClaim("userId"));
        ProjectJoinRequest joinRequest = projectJoinRequestService.createProjectJoinRequest(projectId,currentUserId, request.message());

        return ProjectJoinRequestResponse.toResponse(joinRequest);
    }
}
