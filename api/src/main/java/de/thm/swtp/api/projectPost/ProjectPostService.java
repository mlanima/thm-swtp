package de.thm.swtp.api.projectPost;

import de.thm.swtp.api.exceptionhandling.exceptions.InvalidProjectPostException;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectPostAccessDeniedException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectPostService {
    private final ProjectPostRepository projectPostRepository;
    private final ProjectRepository projectRepository;
    private final UserProfileRepository userProfileRepository;



    @Transactional(readOnly = true)
    public List<ProjectPost> getPublishedPostsForProject(UUID projectId, UUID currentUserId) {
        ProjectEntity projectEntity = getProjectOrThrowError(projectId);

        if (projectEntity.isPrivateProject() && !isProjectContributor(projectEntity, currentUserId)) {
            throw new ProjectPostAccessDeniedException("You are not allowed to view posts of private projects.");
        }

        return projectPostRepository.findAllByProjectIdAndStatusOrderByPublishedAtDesc(projectId, ProjectPostStatus.PUBLISHED)
                .stream()
                .map(ProjectPostMapper::toDomain)
                .toList();
    }



    @Transactional
    public ProjectPost createProjectPost(UUID projectId, UUID authorId, String title, String content, PostContentFormat contentFormat, ProjectPostStatus status) {
        ProjectEntity projectEntity = getProjectOrThrowError(projectId);
        UserProfile author = getUserOrThrowError(authorId);

        checkCanCreatePost(projectEntity,authorId);
        validateCreatePost(status,contentFormat);

        ProjectPostEntity projectPostEntity = ProjectPostEntity.builder()
                .project(projectEntity)
                .author(author)
                .title(title)
                .content(content)
                .contentFormat(contentFormat)
                .status(status)
                .publishedAt(status == ProjectPostStatus.PUBLISHED ? LocalDateTime.now() : null)
                .build();

        return ProjectPostMapper.toDomain(projectPostRepository.saveAndFlush(projectPostEntity));
    }

    @Transactional
    public ProjectPost publishProjectPost(UUID projectId, UUID postId, UUID currentUserId){
        ProjectPostEntity postEntity = getPostOrThrowError(postId);

        if (!postEntity.getProject().getId().equals(projectId)) {
            throw new ProjectPostNotFoundException(postId);
        }

        switch (postEntity.getStatus()) {
            case DRAFT -> checkCanPublishDraft(postEntity, currentUserId);
            case ARCHIVED -> checkCanPublishArchivedPost(postEntity, currentUserId);
            case PUBLISHED -> {return ProjectPostMapper.toDomain(postEntity);}
            default -> throw new InvalidProjectPostException("Invalid post status");
        }

        postEntity.setStatus(ProjectPostStatus.PUBLISHED);

        if (postEntity.getPublishedAt() == null) {
            postEntity.setPublishedAt(LocalDateTime.now());
        }
        postEntity.setArchivedAt(null);

        return ProjectPostMapper.toDomain(projectPostRepository.save(postEntity));
    }

    @Transactional
    public ProjectPost archiveProjectPost(UUID projectId, UUID postId, UUID currentUserId) {
        ProjectPostEntity postEntity = getPostOrThrowError(postId);

        if (!postEntity.getProject().getId().equals(projectId)) {
            throw new ProjectPostNotFoundException(postId);
        }

        checkCanArchivePost(postEntity, currentUserId);

        if (postEntity.getStatus().equals(ProjectPostStatus.ARCHIVED)) {
            return ProjectPostMapper.toDomain(postEntity);
        }

        postEntity.setStatus(ProjectPostStatus.ARCHIVED);
        postEntity.setArchivedAt(LocalDateTime.now());

        return ProjectPostMapper.toDomain(projectPostRepository.save(postEntity));
    }

    @Transactional
    public void deleteProjectPost(UUID projectId, UUID postId, UUID currentUserId) {
        ProjectPostEntity postEntity = getPostOrThrowError(postId);

        if (!postEntity.getProject().getId().equals(projectId)) {
            throw new ProjectPostNotFoundException(postId);
        }

        checkCanDeletePost(postEntity, currentUserId);
        projectPostRepository.delete(postEntity);

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

    private boolean isProjectOwner(ProjectEntity project, UUID userId) {
        return project.getOwner().getKeycloakId().equals(userId);
    }

    private boolean isProjectMember(ProjectEntity project, UUID userId) {
        return project.getMembers()
                .stream()
                .anyMatch(member -> member.getKeycloakId().equals(userId));
    }

    private boolean isPostAuthor(ProjectPostEntity post, UUID userId) {
        return post.getAuthor().getKeycloakId().equals(userId);
    }

    private boolean isProjectContributor(ProjectEntity project, UUID userId) {
        if (userId == null) {
            return false;
        }

        return isProjectOwner(project, userId) || isProjectMember(project, userId);
    }

    private boolean isProjectOwnerOrPostAuthor(ProjectPostEntity post, UUID userId) {
        return isProjectOwner(post.getProject(), userId) || isPostAuthor(post, userId);
    }

    private void checkCanCreatePost(ProjectEntity project, UUID userId) {
        if (!isProjectContributor(project, userId)) {
            throw new ProjectPostAccessDeniedException("Only project members or owners can create posts.");
        }
    }

    private void checkCanDeletePost(ProjectPostEntity post, UUID userId) {
        if (!isProjectOwnerOrPostAuthor(post, userId)) {
            throw new ProjectPostAccessDeniedException("Only the project owner or the post author is allowed to delete posts.");
        }
    }

    private void checkCanPublishDraft(ProjectPostEntity post, UUID userId) {
        if (!isPostAuthor(post, userId)) {
            throw new ProjectPostAccessDeniedException("Only the post author is allowed to publish drafts.");
        }
    }

    private void checkCanPublishArchivedPost(ProjectPostEntity post, UUID userId) {
        if (!isProjectOwnerOrPostAuthor(post, userId)) {
            throw new ProjectPostAccessDeniedException("Only the project owner or the post author is allowed to publish archived posts.");
        }
    }

    private void checkCanArchivePost(ProjectPostEntity post, UUID userId) {
        if (!isProjectOwnerOrPostAuthor(post, userId)) {
            throw new ProjectPostAccessDeniedException("Only the project owner or the post author is allowed to archive posts.");
        }
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


}
