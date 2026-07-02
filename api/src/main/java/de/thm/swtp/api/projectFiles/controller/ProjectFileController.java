package de.thm.swtp.api.projectFiles.controller;

import de.thm.swtp.api.projectFiles.domain.FileVisibility;
import de.thm.swtp.api.projectFiles.domain.ProjectFileDownload;
import de.thm.swtp.api.projectFiles.dto.ProjectFileResponse;
import de.thm.swtp.api.projectFiles.dto.UpdateProjectFileRequest;
import de.thm.swtp.api.projectFiles.service.ProjectFileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/files")
public class ProjectFileController {

    private final ProjectFileService projectFileService;

    @GetMapping
    @PreAuthorize("@security.canViewProjectFiles(#projectId, authentication)")
    public List<ProjectFileResponse> getProjectFiles(@PathVariable UUID projectId, @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = getCurrentUserId(jwt);

        return projectFileService.getProjectFiles(projectId, currentUserId)
                .stream()
                .map(ProjectFileResponse::toResponse)
                .toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@security.canCreateProjectFile(#projectId, authentication)")
    public ProjectFileResponse uploadFile(@PathVariable UUID projectId,
                                          @RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "visibility", required = false) FileVisibility visibility) {
        return ProjectFileResponse.toResponse(projectFileService.uploadFile(projectId, file, visibility));
    }

    @GetMapping("/{fileId}/download")
    @PreAuthorize("@security.canDownloadProjectFile(#projectId, #fileId, authentication)")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID projectId,
                                                 @PathVariable UUID fileId) {
        ProjectFileDownload download = projectFileService.prepareDownload(projectId, fileId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.file().getMimeType()))
                .contentLength(download.file().getSizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.builder("attachment")
                                .filename(download.file().getOriginalName(), StandardCharsets.UTF_8)
                                .build().toString())
                .body(download.resource());
    }

    @PatchMapping("/{fileId}")
    @PreAuthorize("@security.canEditProjectFile(#projectId, authentication)")
    public ProjectFileResponse updateFileVisibility(@PathVariable UUID projectId, @PathVariable UUID fileId,
                                                     @Valid @RequestBody UpdateProjectFileRequest request) {
        return ProjectFileResponse.toResponse(
                projectFileService.updateFileVisibility(projectId, fileId, request.visibility()));
    }

    @DeleteMapping("/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@security.canDeleteProjectFile(#projectId, authentication)")
    public void deleteFile(@PathVariable UUID projectId,
                           @PathVariable UUID fileId) {
        projectFileService.deleteFile(projectId, fileId);
    }

    private UUID getCurrentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

}
