package de.thm.swtp.api.projectInvitation.controller;

import de.thm.swtp.api.discord.service.DiscordProjectService;
import de.thm.swtp.api.projectInvitation.dto.CreateProjectInviteRequest;
import de.thm.swtp.api.projectInvitation.dto.ProjectInviteResponse;
import de.thm.swtp.api.projectInvitation.dto.UpdateProjectInviteStatusRequest;
import de.thm.swtp.api.projectInvitation.service.ProjectInviteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final DiscordProjectService discordProjectService;

    private String getDiscordInviteUrl(UUID projectId) {
        return discordProjectService.getActiveInviteUrl(projectId);
    }

    /** Creates invitation to a project. Only the owner can create the invitation.*/
    @PostMapping("/projects/{projectId}/invitations")
    @PreAuthorize("@security.canCreateProjectInvitation(#projectId, authentication)")
    public ProjectInviteResponse createProjectInvite(@PathVariable UUID projectId,
                                                         @Valid @RequestBody CreateProjectInviteRequest request) {
        return ProjectInviteResponse.toResponse(
                projectInviteService.createProjectInvite(projectId, request.invitedUserId(), request.message()),
                getDiscordInviteUrl(projectId)
        );
    }

    /** Current user gets all his invitations. */
    @GetMapping("/users/me/invitations")
    public List<ProjectInviteResponse> getInvites(@AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        return projectInviteService.getInvitesForUser(currentUserId)
                .stream()
                .map(invite -> ProjectInviteResponse.toResponse(invite, getDiscordInviteUrl(invite.getProjectId())))
                .toList();
    }

    @GetMapping("/projects/{projectId}/invitations")
    @PreAuthorize("@security.canViewProjectInvites(#projectId, authentication)")
    public List<ProjectInviteResponse> getInvitesForProject(@PathVariable UUID projectId) {
        String inviteUrl = getDiscordInviteUrl(projectId);
        return projectInviteService.getInvitesForProject(projectId)
                .stream()
                .map(invite -> ProjectInviteResponse.toResponse(invite, inviteUrl))
                .toList();
    }

    /** Current user can accept or reject an invitation. */
    @PatchMapping("/invitations/{invitationId}")
    @PreAuthorize("@security.canRespondToProjectInvite(#invitationId, authentication)")
    public ProjectInviteResponse updateInviteStatus(@PathVariable UUID invitationId, @Valid @RequestBody UpdateProjectInviteStatusRequest request) {
        ProjectInviteResponse response = ProjectInviteResponse.toResponse(
                projectInviteService.updateInviteStatus(invitationId, request.status())
        );
        return new ProjectInviteResponse(
                response.id(), response.projectId(), response.projectName(),
                response.projectUrl(), response.invitedByUsername(), response.invitedUserId(),
                response.message(), response.status(), response.createdAt(),
                getDiscordInviteUrl(response.projectId())
        );
    }

}


