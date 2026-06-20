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
    public List<ProjectFileResponse> getProjectFiles(@PathVariable UUID projectId) {
        return projectFileService.getProjectFiles(projectId)
                .stream()
                .map(ProjectFileResponse::toResponse)
                .toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectFileResponse uploadFile(@PathVariable UUID projectId,
                                          @AuthenticationPrincipal Jwt jwt,
                                          @RequestParam("file") MultipartFile file) {
        UUID currentUserId = getCurrentUserId(jwt);
        return ProjectFileResponse.toResponse(projectFileService.uploadFile(projectId, currentUserId, file));
    }

    @GetMapping("/{fileId}/download")
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
    public void deleteFile(@PathVariable UUID projectId,
                           @PathVariable UUID fileId,
                           @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = getCurrentUserId(jwt);
        projectFileService.deleteFile(projectId, currentUserId, fileId);
    }

    private UUID getCurrentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
