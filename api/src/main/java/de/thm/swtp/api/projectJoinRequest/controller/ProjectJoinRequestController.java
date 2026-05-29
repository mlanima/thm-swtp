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

/** REST controller for managing project join requests. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ProjectJoinRequestController {
    private final ProjectJoinRequestService projectJoinRequestService;

    /** Creates a join request to the given project for the given authenticated user. */
    @PostMapping("/projects/{projectId}/join-requests")
    public ProjectJoinRequestResponse createProjectJoinRequest(@PathVariable UUID projectId,
                                                               @Valid @RequestBody CreateProjectJoinRequestRequest request,
                                                               @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        ProjectJoinRequest joinRequest = projectJoinRequestService.createProjectJoinRequest(projectId,currentUserId, request.message());

        return ProjectJoinRequestResponse.toResponse(joinRequest);
    }


    @PatchMapping("/project-join-requests/{requestId}/accept")
    public ProjectJoinRequestResponse acceptJoinRequest(@PathVariable UUID requestId, @AuthenticationPrincipal Jwt jwt) {
        UUID currentUser = UUID.fromString(jwt.getSubject());

        ProjectJoinRequest joinRequest = projectJoinRequestService.acceptJoinRequest(requestId, currentUser);

        return ProjectJoinRequestResponse.toResponse(joinRequest);
    }
}
