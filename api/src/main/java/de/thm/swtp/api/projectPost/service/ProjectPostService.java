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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.apache.tika.Tika;

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

    private final Tika tika = new Tika();

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5 MB

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

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

        validateImageSize(image);

        String detectedContentType = detectImageContentType(image);
        String fileExtension = getFileExtension(detectedContentType);
        String fileName = postId + "-" + UUID.randomUUID() + fileExtension;

        Path uploadDir = getPostImageUploadDir();
        Path targetPath = uploadDir.resolve(fileName).normalize();

        String oldFileName = postEntity.getImageFileName();

        try {
            Files.createDirectories(uploadDir);

            if (!targetPath.startsWith(uploadDir.normalize())) {
                throw new InvalidProjectPostException("Invalid image path.");
            }

            Files.copy(image.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            postEntity.setImageFileName(fileName);
            postEntity.setImageUrl("/api/v1/projects/" + projectId + "/posts/" + postId + "/image");

            ProjectPost post = ProjectPostMapper.toDomain(projectPostRepository.save(postEntity));

            deletePostImageFileIfExists(oldFileName);

            TxLogger.afterCommit(log, "Image uploaded for post: project={}, post={}", projectId, postId);

            return post;
        } catch (Exception e) {
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException deleteException) {
                log.warn("Could not delete orphan post image after failed upload: {}", targetPath, deleteException);
            }

            throw new InvalidProjectPostException("Could not store image.");
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> getPostImage(UUID projectId, UUID postId) {
        ProjectPostEntity postEntity = getPostOrThrowError(postId);
        assertPostBelongsToProject(postEntity, projectId);

        String fileName = postEntity.getImageFileName();

        if (fileName == null || fileName.isBlank()) {
            throw new ProjectPostNotFoundException(postId);
        }

        Path uploadDir = getPostImageUploadDir();
        Path imagePath = uploadDir.resolve(fileName).normalize();

        if (!imagePath.startsWith(uploadDir.normalize()) || !Files.exists(imagePath)) {
            throw new ProjectPostNotFoundException(postId);
        }

        try {
            Resource resource = new UrlResource(imagePath.toUri());
            String contentType = Files.probeContentType(imagePath);

            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (IOException e) {
            throw new InvalidProjectPostException("Could not load image.");
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

        String imageFileName = postEntity.getImageFileName();

        projectPostRepository.delete(postEntity);

        deletePostImageFileIfExists(imageFileName);

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

    private void validateImageSize(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new InvalidProjectPostException("Image must not be empty.");
        }

        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new InvalidProjectPostException("Image must not be larger than 5 MB.");
        }
    }

    private String detectImageContentType(MultipartFile image) {
        try {
            String detectedContentType = tika.detect(image.getInputStream());

            if (!ALLOWED_IMAGE_TYPES.contains(detectedContentType)) {
                throw new InvalidProjectPostException("Only JPEG, PNG and WEBP images are allowed.");
            }

            return detectedContentType;
        } catch (IOException e) {
            throw new InvalidProjectPostException("Could not validate image.");
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

    private Path getPostImageUploadDir() {
        return Path.of(uploadsDir, "project-post-images");
    }

    private void deletePostImageFileIfExists(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }

        Path uploadDir = getPostImageUploadDir();
        Path imagePath = uploadDir.resolve(fileName).normalize();

        if (!imagePath.startsWith(uploadDir.normalize())) {
            log.warn("Skipped deleting post image outside upload directory: {}", imagePath);
            return;
        }

        try {
            Files.deleteIfExists(imagePath);
        } catch (IOException e) {
            log.warn("Could not delete post image file: {}", fileName, e);
        }
    }

}
