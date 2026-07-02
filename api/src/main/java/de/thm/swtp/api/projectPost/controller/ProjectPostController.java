package de.thm.swtp.api.projectPost.controller;

import de.thm.swtp.api.projectPost.service.ProjectPostService;
import de.thm.swtp.api.projectPost.dto.CreateProjectPostRequest;
import de.thm.swtp.api.projectPost.dto.ProjectPostResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/posts")
public class ProjectPostController {
    private final ProjectPostService projectPostService;

    @GetMapping
    @PreAuthorize("@security.canViewProject(#projectId, authentication)")
    public List<ProjectPostResponse> getPublishedPosts(@PathVariable UUID projectId) {
        return projectPostService.getPublishedPostsForProject(projectId)
                .stream()
                .map(ProjectPostResponse::toResponse)
                .toList();
    }


    @PostMapping
    @PreAuthorize("@security.canCreateProjectPost(#projectId, authentication)")
    public ProjectPostResponse createPost(@PathVariable UUID projectId, @AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody CreateProjectPostRequest createProjectPostRequest) {

        UUID authorId = getCurrentUserId(jwt);
        return ProjectPostResponse.toResponse(projectPostService.createProjectPost(projectId, authorId,
                createProjectPostRequest.title(), createProjectPostRequest.content(),
                createProjectPostRequest.contentFormat(), createProjectPostRequest.status()));
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@security.canDeleteProjectPost(#projectId, #postId, authentication)")
    public void deletePost(@PathVariable UUID projectId, @PathVariable UUID postId) {
        projectPostService.deleteProjectPost(projectId, postId);
    }

    @PatchMapping("/{postId}/publish")
    @PreAuthorize("@security.canPublishProjectPost(#projectId, #postId, authentication)")
    public ProjectPostResponse publishPost(@PathVariable UUID projectId, @PathVariable UUID postId) {
        return ProjectPostResponse.toResponse(projectPostService.publishProjectPost(projectId, postId));
    }

    @PatchMapping("/{postId}/archive")
    @PreAuthorize("@security.canArchiveProjectPost(#projectId, #postId, authentication)")
    public ProjectPostResponse archivePost(@PathVariable UUID projectId, @PathVariable UUID postId) {
        return ProjectPostResponse.toResponse(projectPostService.archiveProjectPost(projectId, postId));

    }

    @PostMapping("/{postId}/image")
    @PreAuthorize("@security.canCreateProjectPost(#projectId, authentication)")
    public ProjectPostResponse uploadPostImage(
            @PathVariable UUID projectId,
            @PathVariable UUID postId,
            @RequestParam("image") MultipartFile image
    ) {
        return ProjectPostResponse.toResponse(
                projectPostService.uploadPostImage(projectId, postId, image)
        );
    }

    @GetMapping("/{postId}/image")
    @PreAuthorize("@security.canViewProject(#projectId, authentication)")
    public ResponseEntity<Resource> getPostImage(
            @PathVariable UUID projectId,
            @PathVariable UUID postId
    ) {
        return projectPostService.getPostImage(projectId, postId);
    }

    private UUID getCurrentUserId(Jwt jwt){
        return UUID.fromString(jwt.getSubject());
    }
}
