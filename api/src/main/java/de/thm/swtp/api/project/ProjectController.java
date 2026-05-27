package de.thm.swtp.api.project;


import de.thm.swtp.api.project.dto.request.*;
import de.thm.swtp.api.project.dto.response.*;
import lombok.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @RequestBody CreateProjectRequest request,
            @RequestHeader ("X-User-Id")UUID ownerId){
        ProjectResponse response = projectService.createProject(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<DeleteProjectResponse> deleteProject(
            @PathVariable UUID projectId,
            @RequestHeader("X-User-Id") UUID requestingUserId) {
        DeleteProjectResponse response = projectService.deleteProject(projectId, requestingUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable UUID projectId) {
        ProjectResponse response = projectService.getProject(projectId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> editProject(
            @PathVariable UUID projectId,
            @RequestBody UpdateProjectRequest request,
            @RequestHeader("X-User-Id") UUID requestingUserId) {
        ProjectResponse response = projectService.editProject(projectId, request, requestingUserId);
        return ResponseEntity.ok(response);
    }

}
