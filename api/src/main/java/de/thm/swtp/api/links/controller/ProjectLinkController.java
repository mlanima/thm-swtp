package de.thm.swtp.api.links.controller;

import de.thm.swtp.api.links.dto.CreateProjectLinkRequest;
import de.thm.swtp.api.links.dto.ProjectLinkResponse;
import de.thm.swtp.api.links.dto.UpdateProjectLinkRequest;
import de.thm.swtp.api.links.service.ProjectLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/links")
public class ProjectLinkController {
    private final ProjectLinkService projectLinkService;

    @GetMapping
    @PreAuthorize("@security.canViewProjectLinks(#projectId, authentication)")
    public List<ProjectLinkResponse> getProjectLinks(@PathVariable UUID projectId, @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = getCurrentUserId(jwt);

        return projectLinkService.getProjectLinks(projectId, currentUserId)
                .stream()
                .map(ProjectLinkResponse::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@security.canCreateProjectLink(#projectId, authentication)")
    public ProjectLinkResponse createProjectLink(@PathVariable UUID projectId,
                                                 @Valid @RequestBody CreateProjectLinkRequest createProjectLinkRequest) {

        return ProjectLinkResponse.toResponse(projectLinkService.createProjectLink(projectId,
                createProjectLinkRequest.label(), createProjectLinkRequest.url(), createProjectLinkRequest.visibility()));
    }

    @PatchMapping("/{linkId}")
    @PreAuthorize("@security.canEditProjectLink(#projectId, authentication)")
    public ProjectLinkResponse updateProjectLink(@PathVariable UUID projectId, @PathVariable UUID linkId,
                                                 @Valid @RequestBody UpdateProjectLinkRequest updateProjectLinkRequest) {

        return ProjectLinkResponse.toResponse(projectLinkService.updateProjectLink(projectId,
                linkId, updateProjectLinkRequest.label(), updateProjectLinkRequest.url(), updateProjectLinkRequest.visibility()));
    }



    @DeleteMapping("/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@security.canDeleteProjectLink(#projectId, authentication)")
    public void deleteProjectLink(@PathVariable UUID projectId, @PathVariable UUID linkId) {
        projectLinkService.deleteProjectLink(projectId, linkId);
    }

    private UUID getCurrentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
        }

}
