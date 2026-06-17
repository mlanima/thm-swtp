package de.thm.swtp.api.links.controller;

import de.thm.swtp.api.links.dto.CreateLinkRequest;
import de.thm.swtp.api.links.dto.UpdateLinkRequest;
import de.thm.swtp.api.links.dto.UserProfileLinkResponse;
import de.thm.swtp.api.links.service.UserProfileLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/{userId}/links")
public class UserProfileLinkController {
    private final UserProfileLinkService userProfileLinkService;

    @GetMapping
    public List<UserProfileLinkResponse> getUserProfileLinks(@PathVariable UUID userId) {
        return userProfileLinkService.getUserProfileLinks(userId)
                .stream()
                .map(UserProfileLinkResponse::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileLinkResponse createUserProfileLink(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt,
                                                 @Valid @RequestBody CreateLinkRequest createLinkRequest) {
        UUID currentUserId = getCurrentUserId(jwt);

        return UserProfileLinkResponse.toResponse(userProfileLinkService.createUserProfileLink(userId, currentUserId,
                createLinkRequest.label(), createLinkRequest.url()));
    }

    @PatchMapping("/{linkId}")
    public UserProfileLinkResponse updateUserProfileLink(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt, @PathVariable UUID linkId,
                                                 @Valid @RequestBody UpdateLinkRequest updateLinkRequest) {
        UUID currentUserId = getCurrentUserId(jwt);

        return UserProfileLinkResponse.toResponse(userProfileLinkService.updateUserProfileLink(userId, currentUserId,
                linkId, updateLinkRequest.label(), updateLinkRequest.url()));
    }

    @DeleteMapping("/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserProfileLink(@PathVariable UUID userId, @PathVariable UUID linkId, @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = getCurrentUserId(jwt);
        userProfileLinkService.deleteUserProfileLink(userId, currentUserId, linkId);
    }




    private UUID getCurrentUserId(Jwt jwt){
        return UUID.fromString(jwt.getSubject());
    }
}

