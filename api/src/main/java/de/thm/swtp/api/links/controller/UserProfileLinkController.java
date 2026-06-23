package de.thm.swtp.api.links.controller;


import de.thm.swtp.api.links.dto.CreateUserProfileLinkRequest;
import de.thm.swtp.api.links.dto.UpdateUserProfileLinkRequest;
import de.thm.swtp.api.links.dto.UserProfileLinkResponse;
import de.thm.swtp.api.links.service.UserProfileLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("@security.canCreateUserProfileLinks(#userId, authentication)")
    public UserProfileLinkResponse createUserProfileLink(@PathVariable UUID userId,
                                                 @Valid @RequestBody CreateUserProfileLinkRequest userProfileLinkRequest) {
        return UserProfileLinkResponse.toResponse(userProfileLinkService.createUserProfileLink(userId,
                userProfileLinkRequest.label(), userProfileLinkRequest.url()));
    }

    @PatchMapping("/{linkId}")
    @PreAuthorize("@security.canEditUserProfileLinks(#userId, authentication)")
    public UserProfileLinkResponse updateUserProfileLink(@PathVariable UUID userId, @PathVariable UUID linkId,
                                                 @Valid @RequestBody UpdateUserProfileLinkRequest updateUserProfileLinkRequest) {
        return UserProfileLinkResponse.toResponse(userProfileLinkService.updateUserProfileLink(userId,
                linkId, updateUserProfileLinkRequest.label(), updateUserProfileLinkRequest.url()));
    }

    @DeleteMapping("/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@security.canDeleteUserProfileLinks(#userId, authentication)")
    public void deleteUserProfileLink(@PathVariable UUID userId, @PathVariable UUID linkId) {
        userProfileLinkService.deleteUserProfileLink(userId, linkId);
    }

}

