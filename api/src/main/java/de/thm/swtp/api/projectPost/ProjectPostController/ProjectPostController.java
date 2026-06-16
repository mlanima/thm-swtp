package de.thm.swtp.api.projectPost.ProjectPostController;

import de.thm.swtp.api.projectPost.ProjectPostService;
import de.thm.swtp.api.projectPost.dto.CreateProjectPostRequest;
import de.thm.swtp.api.projectPost.dto.ProjectPostResponse;
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
@RequestMapping("/api/v1/projects/{projectId}/posts")
public class ProjectPostController {
    private final ProjectPostService projectPostService;

    @GetMapping
    public List<ProjectPostResponse> getPublishedPosts(@PathVariable UUID projectId, @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = getCurrentUserId(jwt);
        return projectPostService.getPublishedPostsForProject(projectId,currentUserId)
                .stream()
                .map(ProjectPostResponse::toResponse)
                .toList();
    }


    @PostMapping
    public ProjectPostResponse createPost(@PathVariable UUID projectId, @AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody CreateProjectPostRequest createProjectPostRequest) {

        UUID currentUserId = getCurrentUserId(jwt);
        return ProjectPostResponse.toResponse(projectPostService.createProjectPost(projectId,currentUserId,
                createProjectPostRequest.title(), createProjectPostRequest.content(),
                createProjectPostRequest.contentFormat(), createProjectPostRequest.status()));
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable UUID projectId, @PathVariable UUID postId, @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = getCurrentUserId(jwt);
        projectPostService.deleteProjectPost(projectId, postId, currentUserId);
    }

    @PatchMapping("/{postId}/publish")
    public ProjectPostResponse publishPost(@PathVariable UUID projectId, @PathVariable UUID postId, @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = getCurrentUserId(jwt);

        return ProjectPostResponse.toResponse(projectPostService.publishProjectPost(projectId, postId, currentUserId));
    }

    @PatchMapping("/{postId}/archive")
    public ProjectPostResponse archivePost(@PathVariable UUID projectId, @PathVariable UUID postId, @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = getCurrentUserId(jwt);

        return ProjectPostResponse.toResponse(projectPostService.archiveProjectPost(projectId, postId, currentUserId));

    }

    private UUID getCurrentUserId(Jwt jwt){
        return UUID.fromString(jwt.getSubject());
    }
}
