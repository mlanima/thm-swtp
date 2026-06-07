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

import java.util.List;
import java.util.UUID;

/** REST controller for managing project join requests. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ProjectJoinRequestController {
    private final ProjectJoinRequestService projectJoinRequestService;


    /** Returns all join-requests for the given project. */
    @GetMapping("/projects/{projectId}/join-requests")
    public List<ProjectJoinRequestResponse> getProjectJoinRequestsForProject(@PathVariable UUID projectId, @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());

        return projectJoinRequestService.getProjectJoinRequestsForProject(projectId, currentUserId)
                .stream()
                .map(ProjectJoinRequestResponse::toResponse)
                .toList();
    }

    /** Returns all join-requests sent by the given user. */
    @GetMapping("/project-join-requests/me")
    public List<ProjectJoinRequestResponse> getOwnProjectJoinRequests(@AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());

        return projectJoinRequestService.getProjectJoinRequestsFromUser(currentUserId)
                .stream()
                .map(ProjectJoinRequestResponse::toResponse)
                .toList();
    }



    /** Creates a join request to the given project for the given authenticated user. */
    @PostMapping("/projects/{projectId}/join-requests")
    public ProjectJoinRequestResponse createProjectJoinRequest(@PathVariable UUID projectId,
                                                               @Valid @RequestBody CreateProjectJoinRequestRequest request,
                                                               @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        ProjectJoinRequest joinRequest = projectJoinRequestService.createProjectJoinRequest(projectId, currentUserId, request.message());

        return ProjectJoinRequestResponse.toResponse(joinRequest);
    }


    /** Updates a join-request and sets its status to accepted. Only the project owner is allowed to accept the request. */
    @PatchMapping("/project-join-requests/{requestId}/accept")
    public ProjectJoinRequestResponse acceptProjectJoinRequest(@PathVariable UUID requestId, @AuthenticationPrincipal Jwt jwt) {
        UUID currentUser = UUID.fromString(jwt.getSubject());

        ProjectJoinRequest joinRequest = projectJoinRequestService.acceptProjectJoinRequest(requestId, currentUser);

        return ProjectJoinRequestResponse.toResponse(joinRequest);
    }

    /** Updates a join-request and sets its status to rejected. Only the project owner is allowed to reject the request. */
    @PatchMapping("/project-join-requests/{requestId}/reject")
    public ProjectJoinRequestResponse rejectProjectJoinRequest(@PathVariable UUID requestId, @AuthenticationPrincipal Jwt jwt) {
        UUID currentUser = UUID.fromString(jwt.getSubject());

        ProjectJoinRequest joinRequest = projectJoinRequestService.rejectProjectJoinRequest(requestId, currentUser);
        return ProjectJoinRequestResponse.toResponse(joinRequest);
    }
}
