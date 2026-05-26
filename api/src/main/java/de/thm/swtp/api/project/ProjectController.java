package de.thm.swtp.api.project;

import de.thm.swtp.api.project.domain.Project;
import de.thm.swtp.api.project.dto.request.CreateProjectRequest;
import de.thm.swtp.api.project.dto.response.ProjectResponse;
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
        ProjectResponse response = projectService.createProject(request,ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


}
