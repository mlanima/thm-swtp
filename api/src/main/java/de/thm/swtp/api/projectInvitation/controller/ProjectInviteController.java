package de.thm.swtp.api.projectInvitation.controller;

import de.thm.swtp.api.projectInvitation.dto.CreateProjectInviteRequest;
import de.thm.swtp.api.projectInvitation.dto.ProjectInviteResponse;
import de.thm.swtp.api.projectInvitation.dto.UpdateProjectInviteStatusRequest;
import de.thm.swtp.api.projectInvitation.service.ProjectInviteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController()
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProjectInviteController {
    private final ProjectInviteService projectInviteService;

    /** Creates invitation to a project. Only the owner can create the invitation.*/
    @PostMapping("/projects/{projectId}/invitations")
    public ProjectInviteResponse createProjectInvite(@PathVariable UUID projectId,
                                                         @Valid @RequestBody CreateProjectInviteRequest request,
                                                         @AuthenticationPrincipal Jwt jwt) {
        UUID senderId = UUID.fromString(jwt.getSubject());
        return ProjectInviteResponse.toResponse(
                projectInviteService.createProjectInvite(projectId, request.invitedUserId(), request.message(), senderId)
        );
    }

    /** Current user gets all his invitations. */
    @GetMapping("/users/me/invitations")
    public List<ProjectInviteResponse> getInvites(@AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        return projectInviteService.getInvitesForUser(currentUserId)
                .stream()
                .map(ProjectInviteResponse::toResponse)
                .toList();
    }

    @GetMapping("/projects/{projectId}/invitations")
    public List<ProjectInviteResponse> getInvitesForProject(@PathVariable UUID projectId, @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        return projectInviteService.getInvitesForProject(projectId, currentUserId)
                .stream()
                .map(ProjectInviteResponse::toResponse)
                .toList();
    }

    /** Current user can accept or reject an invitation. */
    @PatchMapping("/invitations/{invitationId}")
    public ProjectInviteResponse updateInviteStatus(@PathVariable UUID invitationId, @Valid @RequestBody UpdateProjectInviteStatusRequest request, @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        return ProjectInviteResponse.toResponse(
                projectInviteService.updateInviteStatus(invitationId, request.status(), currentUserId)
        );
    }

}


