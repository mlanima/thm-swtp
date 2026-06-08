package de.thm.swtp.api.project;


import de.thm.swtp.api.project.dto.request.*;
import de.thm.swtp.api.project.dto.response.*;
import lombok.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        ProjectResponse response = projectService.createProject(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<DeleteProjectResponse> deleteProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        DeleteProjectResponse response = projectService.deleteProject(projectId, username);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable UUID projectId) {
        ProjectResponse response = projectService.getProject(projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-url/{projectUrl}")
    public ResponseEntity<ProjectResponse> getProjectByUrl(
            @PathVariable String projectUrl) {
        ProjectResponse response = projectService.getProjectByUrl(projectUrl);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> editProject(
            @PathVariable UUID projectId,
            @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        ProjectResponse response = projectService.editProject(projectId, request, username);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{projectId}/allow-join-requests")
    public ResponseEntity<ProjectResponse> updateAllowJoinRequests(
            @PathVariable UUID projectId,
            @RequestParam boolean allow,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        ProjectResponse response = projectService.updateAllowJoinRequests(projectId, allow, currentUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectId}/members")
    public List<ProjectMemberResponse> getProjectMembers(@PathVariable UUID projectId) {
        return projectService.getProjectMembers(projectId)
                .stream()
                .map(ProjectMemberResponse::toResponse)
                .toList();
    }

    @DeleteMapping("/{projectId}/members/{memberId}")
    public void deleteProjectMember(@PathVariable UUID projectId, @PathVariable UUID memberId, @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        projectService.deleteProjectMember(projectId, currentUserId, memberId);
    }

}
