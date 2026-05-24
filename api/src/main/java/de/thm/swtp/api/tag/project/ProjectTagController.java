package de.thm.swtp.api.tag.project;

import de.thm.swtp.api.tag.Tag;
import de.thm.swtp.api.tag.dto.AddProjectTagRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects/{projectId}/tags")
@RequiredArgsConstructor
public class ProjectTagController {

    private final ProjectTagService projectTagService;

    @PostMapping
    public ResponseEntity<Tag> addTag(@PathVariable String projectId,
                                      @RequestBody AddProjectTagRequest request) {
        return ResponseEntity.ok(projectTagService.addTagToProject(projectId, request));
    }
}
