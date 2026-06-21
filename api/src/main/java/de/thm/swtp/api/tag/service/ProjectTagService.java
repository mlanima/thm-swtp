package de.thm.swtp.api.tag.service;

import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import de.thm.swtp.api.tag.domain.Tag;
import de.thm.swtp.api.tag.entity.TagEntity;
import de.thm.swtp.api.tag.exception.TagAccessDeniedException;
import de.thm.swtp.api.tag.mapper.TagMapper;
import de.thm.swtp.api.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectTagService {

    private final TagRepository tagRepository;
    private final ProjectRepository projectRepository;


    /** Returns a list of all tags assigned to the given project. */
    @Transactional(readOnly = true)
    public List<Tag> getProjectTags(UUID projectId) {
        ProjectEntity projectEntity = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        return projectEntity.getTags()
                .stream()
                .map(TagMapper::toDomain)
                .toList();
    }

    /** Assigns a tag to the given project. If the tag does not exist, then it will be created and then assigned. */
    @Transactional
    public Tag addTagToProject(UUID projectId, String tagName) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        TagEntity tagEntity = getOrCreateTag(tagName);
        project.getTags().add(tagEntity);

        return TagMapper.toDomain(tagEntity);
    }

    /** Removes a tag from the given project. Only the project owner is allowed to remove tags. */
    @Transactional
    public void removeTagFromProject(UUID projectId, String tagName) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        tagRepository.findByNameIgnoreCase(tagName.trim())
                .ifPresent(tag -> project.getTags().remove(tag));
    }

    private TagEntity getOrCreateTag(String tagName) {
        String cleaned = tagName.trim();

        return tagRepository.findByNameIgnoreCase(cleaned)
                .orElseGet(() -> tagRepository.save(new TagEntity(cleaned)));
    }
}
