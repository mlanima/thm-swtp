package de.thm.swtp.api.projectPost.service;

import de.thm.swtp.api.exceptionhandling.exceptions.InvalidProjectPostException;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectPostNotFoundException;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import de.thm.swtp.api.projectPost.domain.PostContentFormat;
import de.thm.swtp.api.projectPost.domain.ProjectPost;
import de.thm.swtp.api.projectPost.domain.ProjectPostStatus;
import de.thm.swtp.api.projectPost.entity.ProjectPostEntity;
import de.thm.swtp.api.projectPost.mapper.ProjectPostMapper;
import de.thm.swtp.api.projectPost.repository.ProjectPostRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectPostService {
    private final ProjectPostRepository projectPostRepository;
    private final ProjectRepository projectRepository;
    private final UserProfileRepository userProfileRepository;



    @Transactional(readOnly = true)
    public List<ProjectPost> getPublishedPostsForProject(UUID projectId) {
        getProjectOrThrowError(projectId);

        return projectPostRepository.findAllByProjectIdAndStatusOrderByPublishedAtDesc(projectId, ProjectPostStatus.PUBLISHED)
                .stream()
                .map(ProjectPostMapper::toDomain)
                .toList();
    }



    @Transactional
    public ProjectPost createProjectPost(UUID projectId, UUID authorId, String title, String content, PostContentFormat contentFormat, ProjectPostStatus status) {
        validateCreatePost(status, contentFormat);

        ProjectEntity projectEntity = getProjectOrThrowError(projectId);
        UserProfile author = getUserOrThrowError(authorId);

        ProjectPostEntity projectPostEntity = ProjectPostEntity.builder()
                .project(projectEntity)
                .author(author)
                .title(title)
                .content(content)
                .contentFormat(contentFormat)
                .status(status)
                .publishedAt(status == ProjectPostStatus.PUBLISHED ? LocalDateTime.now() : null)
                .build();

        ProjectPost post = ProjectPostMapper.toDomain(projectPostRepository.saveAndFlush(projectPostEntity));
        log.info("Post created: project={}, post={}, author={}", projectId, post.getId(), authorId);
        return post;
    }

    @Transactional
    public ProjectPost publishProjectPost(UUID projectId, UUID postId){
        ProjectPostEntity postEntity = getPostOrThrowError(postId);

        assertPostBelongsToProject(postEntity, projectId);

        if (postEntity.getStatus() == ProjectPostStatus.PUBLISHED) {
            return ProjectPostMapper.toDomain(postEntity);
        }

        postEntity.setStatus(ProjectPostStatus.PUBLISHED);

        if (postEntity.getPublishedAt() == null) {
            postEntity.setPublishedAt(LocalDateTime.now());
        }
        postEntity.setArchivedAt(null);

        ProjectPost post = ProjectPostMapper.toDomain(projectPostRepository.save(postEntity));
        log.info("Post published: project={}, post={}", projectId, postId);
        return post;
    }

    @Transactional
    public ProjectPost archiveProjectPost(UUID projectId, UUID postId) {
        ProjectPostEntity postEntity = getPostOrThrowError(postId);

        assertPostBelongsToProject(postEntity, projectId);

        if (postEntity.getStatus().equals(ProjectPostStatus.ARCHIVED)) {
            return ProjectPostMapper.toDomain(postEntity);
        }

        postEntity.setStatus(ProjectPostStatus.ARCHIVED);
        postEntity.setArchivedAt(LocalDateTime.now());

        ProjectPost post = ProjectPostMapper.toDomain(projectPostRepository.save(postEntity));
        log.info("Post archived: project={}, post={}", projectId, postId);
        return post;
    }

    @Transactional
    public void deleteProjectPost(UUID projectId, UUID postId) {
        ProjectPostEntity postEntity = getPostOrThrowError(postId);
        assertPostBelongsToProject(postEntity, projectId);

        projectPostRepository.delete(postEntity);
        log.info("Post deleted: project={}, post={}", projectId, postId);
    }





    private ProjectEntity getProjectOrThrowError(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private UserProfile getUserOrThrowError(UUID userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId.toString()));
    }

    private ProjectPostEntity getPostOrThrowError(UUID postId) {
        return projectPostRepository.findById(postId)
                .orElseThrow(() -> new ProjectPostNotFoundException(postId));
    }


    private void validateCreatePost(ProjectPostStatus status, PostContentFormat contentFormat) {
        if (status == null) {
            throw new InvalidProjectPostException("Post status must not be null.");
        }

        if (status == ProjectPostStatus.ARCHIVED) {
            throw new InvalidProjectPostException("Post cannot be created as archived.");
        }

        if (contentFormat == null) {
            throw new InvalidProjectPostException("Post content format must not be null.");
        }
    }

    private void assertPostBelongsToProject(ProjectPostEntity postEntity, UUID projectId){
        if (!postEntity.getProject().getId().equals(projectId)) {
            throw new ProjectPostNotFoundException(postEntity.getId());
        }
    }


}
