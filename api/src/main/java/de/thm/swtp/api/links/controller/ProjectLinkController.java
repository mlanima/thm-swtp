package de.thm.swtp.api.links.controller;

import de.thm.swtp.api.links.dto.CreateLinkRequest;
import de.thm.swtp.api.links.dto.ProjectLinkResponse;
import de.thm.swtp.api.links.dto.UpdateLinkRequest;
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
    @PreAuthorize("@security.canViewProject(#projectId, authentication)")
    public List<ProjectLinkResponse> getProjectLinks(@PathVariable UUID projectId) {
        return projectLinkService.getProjectLinks(projectId)
                .stream()
                .map(ProjectLinkResponse::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public ProjectLinkResponse createProjectLink(@PathVariable UUID projectId,
                                                 @Valid @RequestBody CreateLinkRequest createLinkRequest) {

        return ProjectLinkResponse.toResponse(projectLinkService.createProjectLink(projectId,
                createLinkRequest.label(), createLinkRequest.url()));
    }

    @PatchMapping("/{linkId}")
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public ProjectLinkResponse updateProjectLink(@PathVariable UUID projectId, @PathVariable UUID linkId,
                                                 @Valid @RequestBody UpdateLinkRequest updateLinkRequest) {

        return ProjectLinkResponse.toResponse(projectLinkService.updateProjectLink(projectId,
                linkId, updateLinkRequest.label(), updateLinkRequest.url()));
    }

    @DeleteMapping("/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public void deleteProjectLink(@PathVariable UUID projectId, @PathVariable UUID linkId) {
        projectLinkService.deleteProjectLink(projectId, linkId);
    }

}
