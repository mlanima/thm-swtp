package de.thm.swtp.api.tag.service;

import de.thm.swtp.api.common.TxLogger;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import de.thm.swtp.api.tag.domain.Tag;
import de.thm.swtp.api.tag.entity.TagEntity;
import de.thm.swtp.api.tag.mapper.TagMapper;
import de.thm.swtp.api.tag.repository.TagRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagTransactionService {

    private final TagRepository tagRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public Tag addTagToProfile(UUID userId, String cleaned) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId.toString()));

        TagEntity tag = getOrCreateTag(cleaned);
        profile.getTags().add(tag);

        TxLogger.afterCommit(log, "Tag added to profile: user={}, tag={}", userId, tag.getName());
        return TagMapper.toDomain(tag);
    }

    @Transactional
    public Tag addTagToProject(UUID projectId, String cleaned) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        TagEntity tag = getOrCreateTag(cleaned);
        project.getTags().add(tag);

        TxLogger.afterCommit(log, "Tag added to project: project={}, tag={}", projectId, tag.getName());
        return TagMapper.toDomain(tag);
    }

    private TagEntity getOrCreateTag(String cleaned) {
        return tagRepository.findByNameIgnoreCase(cleaned)
                .orElseGet(() -> tagRepository.save(new TagEntity(cleaned)));
    }
}
