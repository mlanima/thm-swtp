package de.thm.swtp.api.userprofile.controller;

import de.thm.swtp.api.userprofile.dto.UserProfileRequest;
import de.thm.swtp.api.userprofile.dto.UserProfileResponse;
import de.thm.swtp.api.userprofile.mapper.UserProfileMapper;
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
    private final UserProfileMapper userProfileMapper;

    @PostMapping("/api/users/me")
    public UserProfileResponse syncProfile(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        return userProfileMapper.toResponse(
                userProfileService.getOrCreateProfile(keycloakId, username, email)
        );
    }

    @GetMapping("/api/users/{keycloakId}/profile")
    public UserProfileResponse getProfile(@PathVariable String keycloakId) {
        return userProfileMapper.toResponse(userProfileService.getProfile(keycloakId));
    }

    @PutMapping("/api/users/{keycloakId}/profile")
    public UserProfileResponse updateProfile(
            @PathVariable String keycloakId,
            @RequestBody UserProfileRequest request) {
        return userProfileMapper.toResponse(
                userProfileService.updateProfile(keycloakId, request.about(), request.experience())
        );
    }

    @DeleteMapping("/api/users/{keycloakId}/profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile(@PathVariable String keycloakId) {
        userProfileService.deleteProfile(keycloakId);
    }
}
