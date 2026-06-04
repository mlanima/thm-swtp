package de.thm.swtp.api.projectFavorite.controller;

import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.dto.response.ProjectResponse;
import de.thm.swtp.api.projectFavorite.service.ProjectFavoriteService;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users/me/favorites")
@RequiredArgsConstructor
public class ProjectFavoriteController {

    private final ProjectFavoriteService projectFavoriteService;

    @PostMapping("/{projectId}")
    public ResponseEntity<Void> addFavorite(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        projectFavoriteService.addFavorite(projectId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        projectFavoriteService.removeFavorite(projectId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<Void> isFavorited(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        boolean favorited = projectFavoriteService.isFavorited(projectId, userId);
        return favorited ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getFavorites(
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<ProjectResponse> response = projectFavoriteService.getFavorites(userId)
                .stream()
                .map(this::toProjectResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    // project module has no domain/mapper layer — mapping lives here until that's added
    private ProjectResponse toProjectResponse(ProjectEntity project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .projectUrl(project.getProjectUrl())
                .isPrivateProject(project.isPrivateProject())
                .ownerId(project.getOwner().getKeycloakId())
                .memberIds(project.getMembers().stream()
                        .map(UserProfile::getKeycloakId)
                        .collect(Collectors.toSet()))
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
