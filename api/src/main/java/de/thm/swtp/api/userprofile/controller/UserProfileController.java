package de.thm.swtp.api.userprofile.controller;

import de.thm.swtp.api.exceptionhandling.exceptions.ProfileAccessDeniedException;
import de.thm.swtp.api.userprofile.dto.UserProfileRequest;
import de.thm.swtp.api.userprofile.dto.UserProfileResponse;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.service.UserProfileService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;
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
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        return toResponse(userProfileService.getOrCreateProfile(keycloakId, username, email));
    }

    @GetMapping("/api/users/{username}/profile")
    public UserProfileResponse getProfile(@PathVariable String username) {
        return toResponse(userProfileService.getProfile(username));
    }

    @PutMapping("/api/users/{username}/profile")
    public UserProfileResponse updateProfile(
            @PathVariable String username,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UserProfileRequest request) {
        verifyOwnership(username, jwt);
        return toResponse(userProfileService.updateProfile(username, request.title(), request.location(), request.about(), request.experience()));
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

    private UserProfileResponse toResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .keycloakId(profile.getKeycloakId())
                .username(profile.getUsername())
                .email(profile.getEmail())
                .title(profile.getTitle())
                .location(profile.getLocation())
                .followers(profile.getFollowers())
                .about(profile.getAbout())
                .experience(profile.getExperience())
                .build();
    }
}
