package de.thm.swtp.api.tag.project;

import de.thm.swtp.api.project.Project;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.tag.TagRepository;
import de.thm.swtp.api.tag.dto.AddProjectTagRequest;
import de.thm.swtp.api.tag.entity.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectTagService {

    private final TagRepository tagRepository;
    private final ProjectRepository projectTagRepository;


    @Transactional
    public Tag addTagToProject(UUID projectId, AddProjectTagRequest request) {
        Project project = projectTagRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        String name = request.getName().trim();

        Tag tag = tagRepository.findByName(name)
                .orElseGet(() -> tagRepository.save(new Tag(name)));

        project.getTags().add(tag);

        return tag;
    }
}
