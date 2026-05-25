package de.thm.swtp.api.tag.project;

import de.thm.swtp.api.tag.dto.AddProjectTagRequest;
import de.thm.swtp.api.tag.entity.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}/tags")
@RequiredArgsConstructor
public class ProjectTagController {

    private final ProjectTagService projectTagService;

    @PostMapping
    public ResponseEntity<Tag> addTag(@PathVariable UUID projectId,
                                      @RequestBody AddProjectTagRequest request) {
        return ResponseEntity.ok(projectTagService.addTagToProject(projectId, request));
    }
}
