package de.thm.swtp.api.tag.controller;

import de.thm.swtp.api.tag.dto.CreateTagRequest;
import de.thm.swtp.api.tag.dto.TagResponse;
import de.thm.swtp.api.tag.service.ProjectTagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/tags")
@RequiredArgsConstructor
public class ProjectTagController {

    private final ProjectTagService projectTagService;


    /** Returns a list of all tags assigned to a project.*/
    @GetMapping
    @PreAuthorize("@security.canViewProject(#projectId, authentication)")
    public List<TagResponse> getProjectTags(@PathVariable UUID projectId) {
        return projectTagService.getProjectTags(projectId)
                .stream()
                .map(TagResponse::toResponse)
                .toList();
    }

    /** Adds a tag to given project. Only the project owner is allowed to add tags.*/
    @PostMapping
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public TagResponse addTagToProject(@PathVariable UUID projectId, @Valid @RequestBody CreateTagRequest request) {
        return TagResponse.toResponse(projectTagService.addTagToProject(projectId, request.name()));
    }

    /** Removes a tag from given project. Only the project owner is allowed to remove tags. */
    @DeleteMapping("/{tagName}")
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public ResponseEntity<Void> removeTagFromProject(@PathVariable UUID projectId, @PathVariable String tagName) {
        projectTagService.removeTagFromProject(projectId, tagName);
        return ResponseEntity.noContent().build();
    }
}