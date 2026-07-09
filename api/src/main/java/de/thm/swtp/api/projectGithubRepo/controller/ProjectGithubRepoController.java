package de.thm.swtp.api.projectGithubRepo.controller;

import de.thm.swtp.api.projectGithubRepo.dto.GithubReadmeResponse;
import de.thm.swtp.api.projectGithubRepo.dto.GithubRepoCardResponse;
import de.thm.swtp.api.projectGithubRepo.dto.LinkGithubRepoRequest;
import de.thm.swtp.api.projectGithubRepo.dto.SetAutoInviteRequest;
import de.thm.swtp.api.projectGithubRepo.dto.SetReadmeVisibilityRequest;
import de.thm.swtp.api.projectGithubRepo.service.ProjectGithubRepoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/github-repo")
public class ProjectGithubRepoController {

    private final ProjectGithubRepoService projectGithubRepoService;

    @PutMapping
    @PreAuthorize("@security.canManageProjectGithubRepo(#projectId, authentication)")
    public GithubRepoCardResponse linkRepo(
            @PathVariable UUID projectId,
            @Valid @RequestBody LinkGithubRepoRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        return GithubRepoCardResponse.toResponse(
                projectGithubRepoService.link(projectId, currentUserId, request.repoOwner(), request.repoName()));
    }

    @GetMapping
    @PreAuthorize("@security.canViewProjectGithubRepo(#projectId, authentication)")
    public GithubRepoCardResponse getRepo(@PathVariable UUID projectId) {
        return GithubRepoCardResponse.toResponse(projectGithubRepoService.getCard(projectId));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@security.canManageProjectGithubRepo(#projectId, authentication)")
    public void unlinkRepo(@PathVariable UUID projectId) {
        projectGithubRepoService.unlink(projectId);
    }

    @GetMapping("/readme")
    @PreAuthorize("@security.canViewProjectGithubRepo(#projectId, authentication)")
    public GithubReadmeResponse getReadme(@PathVariable UUID projectId) {
        return GithubReadmeResponse.toResponse(projectGithubRepoService.getReadme(projectId));
    }

    @PatchMapping("/readme-visibility")
    @PreAuthorize("@security.canManageProjectGithubRepo(#projectId, authentication)")
    public GithubRepoCardResponse setReadmeVisibility(
            @PathVariable UUID projectId,
            @Valid @RequestBody SetReadmeVisibilityRequest request) {
        return GithubRepoCardResponse.toResponse(projectGithubRepoService.setReadmeVisibility(projectId, request.show()));
    }

    @PatchMapping("/auto-invite")
    @PreAuthorize("@security.canManageProjectGithubRepo(#projectId, authentication)")
    public GithubRepoCardResponse setAutoInviteCollaborators(
            @PathVariable UUID projectId,
            @Valid @RequestBody SetAutoInviteRequest request) {
        return GithubRepoCardResponse.toResponse(
                projectGithubRepoService.setAutoInviteCollaborators(projectId, request.enabled()));
    }
}
