package de.thm.swtp.api.userprofile.controller;

import de.thm.swtp.api.exceptionhandling.exceptions.AccessDeniedException;
import de.thm.swtp.api.userprofile.dto.UserProfileRequest;
import de.thm.swtp.api.userprofile.dto.UserProfileResponse;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping("/api/users/me")
    public UserProfileResponse syncProfile(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        return toResponse(userProfileService.getOrCreateProfile(keycloakId, username, email));
    }

    @GetMapping("/api/users/{keycloakId}/profile")
    public UserProfileResponse getProfile(@PathVariable String keycloakId) {
        return toResponse(userProfileService.getProfile(keycloakId));
    }

    @PutMapping("/api/users/{keycloakId}/profile")
    public UserProfileResponse updateProfile(
            @PathVariable String keycloakId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UserProfileRequest request) {
        verifyOwnership(keycloakId, jwt);
        return toResponse(userProfileService.updateProfile(keycloakId, request.about(), request.experience()));
    }

    @DeleteMapping("/api/users/{keycloakId}/profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile(@PathVariable String keycloakId, @AuthenticationPrincipal Jwt jwt) {
        verifyOwnership(keycloakId, jwt);
        userProfileService.deleteProfile(keycloakId);
    }

    private void verifyOwnership(String keycloakId, Jwt jwt) {
        if (!jwt.getSubject().equals(keycloakId)) {
            throw new AccessDeniedException();
        }
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .keycloakId(profile.getKeycloakId())
                .username(profile.getUsername())
                .email(profile.getEmail())
                .about(profile.getAbout())
                .experience(profile.getExperience())
                .build();
    }
}
