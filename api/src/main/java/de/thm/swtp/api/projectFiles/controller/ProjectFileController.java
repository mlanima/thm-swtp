package de.thm.swtp.api.projectFiles.controller;

import de.thm.swtp.api.projectFiles.domain.ProjectFileDownload;
import de.thm.swtp.api.projectFiles.dto.ProjectFileResponse;
import de.thm.swtp.api.projectFiles.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("@security.canViewProject(#projectId, authentication)")
    public List<ProjectFileResponse> getProjectFiles(@PathVariable UUID projectId) {
        return projectFileService.getProjectFiles(projectId)
                .stream()
                .map(ProjectFileResponse::toResponse)
                .toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public ProjectFileResponse uploadFile(@PathVariable UUID projectId,
                                          @RequestParam("file") MultipartFile file) {
        return ProjectFileResponse.toResponse(projectFileService.uploadFile(projectId, file));
    }

    @GetMapping("/{fileId}/download")
    @PreAuthorize("@security.canViewProject(#projectId, authentication)")
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

    @DeleteMapping("/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public void deleteFile(@PathVariable UUID projectId,
                           @PathVariable UUID fileId) {
        projectFileService.deleteFile(projectId, fileId);
    }

}
