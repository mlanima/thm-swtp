package de.thm.swtp.api.projectFiles.service;

import de.thm.swtp.api.common.LogSafe;
import de.thm.swtp.api.common.TxLogger;
import de.thm.swtp.api.exceptionhandling.exceptions.*;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import de.thm.swtp.api.projectFiles.domain.ProjectFile;
import de.thm.swtp.api.projectFiles.domain.ProjectFileDownload;
import de.thm.swtp.api.projectFiles.entity.ProjectFileEntity;
import de.thm.swtp.api.projectFiles.mapper.ProjectFileMapper;
import de.thm.swtp.api.projectFiles.repository.ProjectFileRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectFileService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "text/plain",
            "text/markdown",
            "text/x-markdown",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final int MAX_FILES_PER_PROJECT = 20;

    @Value("${app.uploads.dir:./uploads}")
    private String uploadDirPath;

    private Path uploadDir;

    private static final Tika TIKA = new Tika();

    private final ProjectFileRepository projectFileRepository;
    private final ProjectRepository projectRepository;

    @PostConstruct
    public void init() {
        try {
            this.uploadDir = Paths.get(uploadDirPath);
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadDirPath, e);
        }
    }

    @Transactional(readOnly = true)
    public List<ProjectFile> getProjectFiles(UUID projectId) {
        getProjectOrThrow(projectId);
        return projectFileRepository.findByProjectIdOrderByCreatedAtAsc(projectId)
                .stream()
                .map(ProjectFileMapper::toDomain)
                .toList();
    }

    @Transactional
    public ProjectFile uploadFile(UUID projectId, MultipartFile file) {
        ProjectEntity project = getProjectOrThrow(projectId);

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new ProjectFileTypeNotAllowedException(mimeType);
        }

        if (projectFileRepository.countByProjectId(projectId) >= MAX_FILES_PER_PROJECT) {
            throw new ProjectFileUploadLimitExceededException(MAX_FILES_PER_PROJECT);
        }

        String rawName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "file";
        String originalName = rawName.length() > 255 ? rawName.substring(0, 255) : rawName;
        String logName = LogSafe.clean(originalName); // strip control chars for log output — originalName is client-controlled
        String extension = getExtension(originalName);
        String storageName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        Path filePath = uploadDir.resolve(storageName);

        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Upload failed (write): project={}, file={}", projectId, logName, e);
            throw new RuntimeException("Failed to write file to storage", e);
        }

        // verify actual MIME type via magic bytes — client-supplied Content-Type is not trusted
        try {
            String detectedMime = TIKA.detect(filePath);
            if (!ALLOWED_MIME_TYPES.contains(detectedMime)) {
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException ignored) {
                }
                throw new ProjectFileTypeNotAllowedException(detectedMime);
            }
            mimeType = detectedMime;
        } catch (ProjectFileTypeNotAllowedException e) {
            throw e;
        } catch (IOException e) {
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException ignored) {
            }
            throw new RuntimeException("Failed to detect MIME type", e);
        }

        ProjectFileEntity entity = ProjectFileEntity.builder()
                .project(project)
                .originalName(originalName)
                .storageName(storageName)
                .mimeType(mimeType)
                .sizeBytes(file.getSize())
                .build();

        try {
            ProjectFile saved = ProjectFileMapper.toDomain(projectFileRepository.save(entity));
            TxLogger.afterCommit(log, "Upload: project={}, file={}, bytes={}, mime={}",
                    projectId, logName, file.getSize(), mimeType);
            return saved;
        } catch (Exception e) {
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException ignored) {
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public ProjectFileDownload prepareDownload(UUID projectId, UUID fileId) {
        getProjectOrThrow(projectId);
        ProjectFileEntity fileEntity = getFileOrThrow(fileId);
        checkFileBelongsToProject(fileEntity, projectId);

        Path filePath = uploadDir.resolve(fileEntity.getStorageName());
        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            throw new ProjectFileNotFoundException(fileId);
        }
        return new ProjectFileDownload(ProjectFileMapper.toDomain(fileEntity), resource);
    }

    @Transactional
    public void deleteFile(UUID projectId, UUID fileId) {
        getProjectOrThrow(projectId);

        ProjectFileEntity fileEntity = getFileOrThrow(fileId);
        checkFileBelongsToProject(fileEntity, projectId);

        // DB first: a DB failure rolls back cleanly; disk is untouched.
        // Disk second: a disk failure leaves an orphaned file (wasted space),
        // but never a DB record pointing to a missing file (unrecoverable crash on next download).
        projectFileRepository.delete(fileEntity);
        try {
            boolean removed = Files.deleteIfExists(uploadDir.resolve(fileEntity.getStorageName()));
            if (!removed) {
                log.warn("Delete: file missing on disk, DB record removed: project={}, file={}",
                        projectId, fileId);
            } else {
                TxLogger.afterCommit(log, "Delete: project={}, file={}, storage={}",
                        projectId, fileId, fileEntity.getStorageName());
            }
        } catch (IOException e) {
            log.error("Delete failed (disk): project={}, file={}", projectId, fileId, e);
            throw new RuntimeException("Failed to delete file from storage", e);
        }
    }

    private ProjectEntity getProjectOrThrow(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private ProjectFileEntity getFileOrThrow(UUID fileId) {
        return projectFileRepository.findById(fileId)
                .orElseThrow(() -> new ProjectFileNotFoundException(fileId));
    }


    private void checkFileBelongsToProject(ProjectFileEntity file, UUID projectId) {
        if (!file.getProject().getId().equals(projectId)) {
            throw new ProjectFileDoesNotBelongToProjectException();
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        String ext = filename.substring(dotIndex + 1);
        return ext.matches("[A-Za-z0-9]{1,10}") ? ext : "";
    }
}
