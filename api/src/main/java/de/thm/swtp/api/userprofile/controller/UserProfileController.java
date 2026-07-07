package de.thm.swtp.api.userprofile.controller;

import de.thm.swtp.api.project.ProjectService;
import de.thm.swtp.api.project.dto.response.ProjectResponse;
import de.thm.swtp.api.userprofile.dto.UserProfileRequest;
import de.thm.swtp.api.userprofile.dto.UserProfileResponse;
import de.thm.swtp.api.userprofile.dto.UserStatusResponse;
import de.thm.swtp.api.userprofile.mapper.UserProfileMapper;
import de.thm.swtp.api.userprofile.mapper.UserStatusMapper;
import de.thm.swtp.api.userprofile.service.UserProfileService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserProfileMapper userProfileMapper;
    private final UserStatusMapper userStatusMapper;
    private final ProjectService projectService;

    @PostMapping("/api/v1/users/me")
    public UserProfileResponse syncProfile(@AuthenticationPrincipal Jwt jwt) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        return userProfileMapper.toResponse(userProfileService.getOrCreateProfile(keycloakId, username, email));
    }

    @GetMapping("/api/v1/users/{username}/profile")
    public UserProfileResponse getProfile(@PathVariable String username) {
        return userProfileMapper.toResponse(userProfileService.getProfile(username));
    }

    @GetMapping("/api/v1/users/{username}/projects")
    @PreAuthorize("@security.canViewUserProjects(#username, authentication)")
    public List<ProjectResponse> getProjects(@PathVariable String username) {
        return projectService.getProjectsByUsername(username);
    }

    @GetMapping("/api/v1/users/{username}/projects/all")
    @PreAuthorize("@security.canViewUserProjects(#username, authentication)")
    public List<ProjectResponse> getAllProjects(@PathVariable String username) {
        return projectService.getAllProjectsByUsername(username);
    }

    @PutMapping("/api/v1/users/{username}/profile")
    @PreAuthorize("@security.canEditUserProfile(#username,authentication)")
    public UserProfileResponse updateProfile(
            @PathVariable String username,
            @RequestBody UserProfileRequest request) {
        return userProfileMapper.toResponse(userProfileService.updateProfile(username, request.title(), request.location(), request.about(), request.experience()));
    }

    @DeleteMapping("/api/v1/users/{username}/profile")
    @PreAuthorize("@security.canDeleteUserProfile(#username, authentication)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile(@PathVariable String username) {
        userProfileService.deleteProfile(username);
    }



    @GetMapping("/api/v1/users/me/ban-status")
    public UserStatusResponse getCurrentUserBanStatus(@AuthenticationPrincipal Jwt jwt) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());

        return userProfileService.findProfileByKeycloakId(keycloakId)
                .map(userStatusMapper::toBannedResponse)
                .orElseGet(userStatusMapper::toNotBannedResponse);
    }
}
