package de.thm.swtp.api.tag.service;

import de.thm.swtp.api.common.TxLogger;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import de.thm.swtp.api.tag.domain.Tag;
import de.thm.swtp.api.tag.exception.TagNotValidException;
import de.thm.swtp.api.tag.mapper.TagMapper;
import de.thm.swtp.api.tag.repository.TagRepository;
import de.thm.swtp.api.tag.validation.TagValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectTagService {

    private final TagRepository tagRepository;
    private final ProjectRepository projectRepository;
    private final TagValidationService tagValidationService;
    private final TagTransactionService tagTransactionService;


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
    public Tag addTagToProject(UUID projectId, String tagName) {
        String cleaned = tagName.trim();

        if (tagRepository.findByNameIgnoreCase(cleaned).isEmpty()) {
            if (!tagValidationService.isValidTag(cleaned)) {
                throw new TagNotValidException(cleaned);
            }
        }

        return tagTransactionService.addTagToProject(projectId, cleaned);
    }

    /** Removes a tag from the given project. Only the project owner is allowed to remove tags. */
    @Transactional
    public void removeTagFromProject(UUID projectId, String tagName) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        tagRepository.findByNameIgnoreCase(tagName.trim())
                .ifPresent(tag -> {
                    project.getTags().remove(tag);
                    TxLogger.afterCommit(log, "Tag removed from project: project={}, tag={}", projectId, tag.getName());
                });
    }
}
