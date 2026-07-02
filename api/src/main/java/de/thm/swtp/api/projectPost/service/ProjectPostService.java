package de.thm.swtp.api.projectPost.service;

import de.thm.swtp.api.common.TxLogger;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.Set;
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

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5 MB

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final Path POST_IMAGE_UPLOAD_DIR = Path.of("uploads/project-post-images");

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
        TxLogger.afterCommit(log, "Post created: project={}, post={}, author={}", projectId, post.getId(), authorId);
        return post;
    }

    @Transactional
    public ProjectPost uploadPostImage(UUID projectId, UUID postId, MultipartFile image) {
        ProjectPostEntity postEntity = getPostOrThrowError(postId);
        assertPostBelongsToProject(postEntity, projectId);

        validateImage(image);

        String contentType = image.getContentType();
        String fileExtension = getFileExtension(contentType);
        String fileName = postId + "-" + UUID.randomUUID() + fileExtension;

        try {
            Files.createDirectories(POST_IMAGE_UPLOAD_DIR);

            Path targetPath = POST_IMAGE_UPLOAD_DIR.resolve(fileName).normalize();

            if (!targetPath.startsWith(POST_IMAGE_UPLOAD_DIR)) {
                throw new InvalidProjectPostException("Invalid image path.");
            }

            Files.copy(image.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = "/uploads/project-post-images/" + fileName;
            postEntity.setImageUrl(imageUrl);

            ProjectPost post = ProjectPostMapper.toDomain(projectPostRepository.save(postEntity));

            TxLogger.afterCommit(log, "Image uploaded for post: project={}, post={}", projectId, postId);

            return post;
        } catch (IOException e) {
            throw new InvalidProjectPostException("Could not store image.");
        }
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
        TxLogger.afterCommit(log, "Post published: project={}, post={}", projectId, postId);
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
        TxLogger.afterCommit(log, "Post archived: project={}, post={}", projectId, postId);
        return post;
    }

    @Transactional
    public void deleteProjectPost(UUID projectId, UUID postId) {
        ProjectPostEntity postEntity = getPostOrThrowError(postId);
        assertPostBelongsToProject(postEntity, projectId);

        projectPostRepository.delete(postEntity);
        TxLogger.afterCommit(log, "Post deleted: project={}, post={}", projectId, postId);
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

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new InvalidProjectPostException("Image must not be empty.");
        }

        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new InvalidProjectPostException("Image must not be larger than 5 MB.");
        }

        String contentType = image.getContentType();

        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new InvalidProjectPostException("Only JPEG, PNG and WEBP images are allowed.");
        }
    }

    private String getFileExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new InvalidProjectPostException("Unsupported image type.");
        };
    }

    private void assertPostBelongsToProject(ProjectPostEntity postEntity, UUID projectId){
        if (!postEntity.getProject().getId().equals(projectId)) {
            throw new ProjectPostNotFoundException(postEntity.getId());
        }
    }


}
