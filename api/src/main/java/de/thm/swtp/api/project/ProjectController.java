package de.thm.swtp.api.project;


import de.thm.swtp.api.project.dto.request.*;
import de.thm.swtp.api.project.dto.response.*;
import lombok.*;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @PreAuthorize("@security.hasModeratorRole(authentication)")
    public ResponseEntity<Page<ProjectResponse>> getAllProjects(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(projectService.getAllProjects(name, pageable));
    }

    @PostMapping
    @PreAuthorize("@security.canCreateProject(authentication)")
    public ResponseEntity<ProjectResponse> createProject(
            @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        ProjectResponse response = projectService.createProject(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{projectId}")
    @PreAuthorize("@security.canDeleteProject(#projectId, authentication)")
    public ResponseEntity<DeleteProjectResponse> deleteProject(@PathVariable UUID projectId) {
        DeleteProjectResponse response = projectService.deleteProject(projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("@security.canViewProject(#projectId, authentication)")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable UUID projectId) {
        ProjectResponse response = projectService.getProject(projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-url/{projectUrl}")
    @PreAuthorize("@security.canViewProjectByUrl(#projectUrl, authentication)")
    public ResponseEntity<ProjectResponse> getProjectByUrl(
            @PathVariable String projectUrl) {
        ProjectResponse response = projectService.getProjectByUrl(projectUrl);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{projectId}")
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public ResponseEntity<ProjectResponse> editProject(
            @PathVariable UUID projectId,
            @RequestBody UpdateProjectRequest request) {
        ProjectResponse response = projectService.editProject(projectId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{projectId}/allow-join-requests")
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public ResponseEntity<ProjectResponse> updateAllowJoinRequests(
            @PathVariable UUID projectId,
            @RequestParam boolean allow) {
        ProjectResponse response = projectService.updateAllowJoinRequests(projectId, allow);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectId}/members")
    @PreAuthorize("@security.canViewProjectMembers(#projectId, authentication)")
    public List<ProjectMemberResponse> getProjectMembers(@PathVariable UUID projectId) {
        return projectService.getProjectMembers(projectId)
                .stream()
                .map(ProjectMemberResponse::toResponse)
                .toList();
    }

    @DeleteMapping("/{projectId}/members/{memberId}")
    @PreAuthorize("@security.canRemoveProjectMember(#projectId, #memberId, authentication)")
    public void deleteProjectMember(@PathVariable UUID projectId, @PathVariable UUID memberId) {
        projectService.deleteProjectMember(projectId, memberId);
    }

    @GetMapping("/url-exists/{projectUrl}")
    public ResponseEntity<Boolean> projectUrlExists(@PathVariable String projectUrl) {
        return ResponseEntity.ok(projectService.projectUrlExists(projectUrl));
    }

}
