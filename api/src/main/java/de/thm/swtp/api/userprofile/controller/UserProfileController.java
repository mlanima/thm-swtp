package de.thm.swtp.api.userprofile.controller;

import de.thm.swtp.api.exceptionhandling.exceptions.ProfileAccessDeniedException;
import de.thm.swtp.api.project.ProjectService;
import de.thm.swtp.api.project.dto.response.ProjectResponse;
import de.thm.swtp.api.userprofile.dto.UserProfileRequest;
import de.thm.swtp.api.userprofile.dto.UserProfileResponse;
import de.thm.swtp.api.userprofile.mapper.UserProfileMapper;
import de.thm.swtp.api.userprofile.service.UserProfileService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserProfileMapper userProfileMapper;
    private final ProjectService projectService;

    @PostMapping("/api/users/me")
    public UserProfileResponse syncProfile(@AuthenticationPrincipal Jwt jwt) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        return userProfileMapper.toResponse(userProfileService.getOrCreateProfile(keycloakId, username, email));
    }

    @GetMapping("/api/users/{username}/profile")
    public UserProfileResponse getProfile(@PathVariable String username) {
        return userProfileMapper.toResponse(userProfileService.getProfile(username));
    }

    @GetMapping("/api/users/{username}/projects")
    public List<ProjectResponse> getProjects(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        verifyOwnership(username, jwt);
        return projectService.getProjectsByUsername(username);
    }

    @PutMapping("/api/users/{username}/profile")
    public UserProfileResponse updateProfile(
            @PathVariable String username,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UserProfileRequest request) {
        verifyOwnership(username, jwt);
        return userProfileMapper.toResponse(userProfileService.updateProfile(username, request.title(), request.location(), request.about(), request.experience()));
    }

    @DeleteMapping("/api/users/{username}/profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        verifyOwnership(username, jwt);
        userProfileService.deleteProfile(username);
    }

    private void verifyOwnership(String username, Jwt jwt) {
        if (!jwt.getClaimAsString("preferred_username").equals(username)) {
            throw new ProfileAccessDeniedException();
        }
    }

}
