package de.thm.swtp.api.links.controller;

import de.thm.swtp.api.links.dto.CreateProjectLinkRequest;
import de.thm.swtp.api.links.dto.ProjectLinkResponse;
import de.thm.swtp.api.links.dto.UpdateProjectLinkRequest;
import de.thm.swtp.api.links.service.ProjectLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public List<ProjectLinkResponse> getProjectLinks(@PathVariable UUID projectId, @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = getCurrentUserId(jwt);

        return projectLinkService.getProjectLinks(projectId, currentUserId)
                .stream()
                .map(ProjectLinkResponse::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectLinkResponse createProjectLink(@PathVariable UUID projectId, @AuthenticationPrincipal Jwt jwt,
                                                 @Valid @RequestBody CreateProjectLinkRequest createProjectLinkRequest) {
        UUID currentUserId = getCurrentUserId(jwt);

        return ProjectLinkResponse.toResponse(projectLinkService.createProjectLink(projectId, currentUserId,
                createProjectLinkRequest.label(), createProjectLinkRequest.url(), createProjectLinkRequest.visibility()));
    }

    @PatchMapping("/{linkId}")
    public ProjectLinkResponse updateProjectLink(@PathVariable UUID projectId, @AuthenticationPrincipal Jwt jwt, @PathVariable UUID linkId,
                                                 @Valid @RequestBody UpdateProjectLinkRequest updateProjectLinkRequest) {
        UUID currentUserId = getCurrentUserId(jwt);

        return ProjectLinkResponse.toResponse(projectLinkService.updateProjectLink(projectId, currentUserId,
                linkId, updateProjectLinkRequest.label(), updateProjectLinkRequest.url(), updateProjectLinkRequest.visibility()));
    }

    @DeleteMapping("/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProjectLink(@PathVariable UUID projectId, @PathVariable UUID linkId, @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = getCurrentUserId(jwt);
        projectLinkService.deleteProjectLink(projectId, currentUserId, linkId);
    }




    private UUID getCurrentUserId(Jwt jwt){
        return UUID.fromString(jwt.getSubject());
    }
}
