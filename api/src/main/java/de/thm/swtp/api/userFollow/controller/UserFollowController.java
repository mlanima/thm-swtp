package de.thm.swtp.api.userFollow.controller;

import de.thm.swtp.api.userFollow.service.UserFollowService;
import de.thm.swtp.api.userprofile.dto.UserProfileResponse;
import de.thm.swtp.api.userprofile.mapper.UserProfileMapper;
import de.thm.swtp.api.userprofile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserFollowController {

    private final UserFollowService userFollowService;
    private final UserProfileService userProfileService;
    private final UserProfileMapper userProfileMapper;

    @PostMapping("/{username}/follow")
    public ResponseEntity<Void> follow(
            @PathVariable String username,
            @AuthenticationPrincipal Jwt jwt) {
        UUID followerId = UUID.fromString(jwt.getSubject());
        UUID followingId = userProfileService.getProfile(username).getKeycloakId();
        userFollowService.follow(followerId, followingId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{username}/follow")
    public ResponseEntity<Void> unfollow(
            @PathVariable String username,
            @AuthenticationPrincipal Jwt jwt) {
        UUID followerId = UUID.fromString(jwt.getSubject());
        UUID followingId = userProfileService.getProfile(username).getKeycloakId();
        userFollowService.unfollow(followerId, followingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{username}/follow")
    public ResponseEntity<Void> isFollowing(
            @PathVariable String username,
            @AuthenticationPrincipal Jwt jwt) {
        UUID followerId = UUID.fromString(jwt.getSubject());
        UUID followingId = userProfileService.getProfile(username).getKeycloakId();
        boolean following = userFollowService.isFollowing(followerId, followingId);
        return following ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/{username}/followers")
    public List<UserProfileResponse> getFollowers(@PathVariable String username) {
        UUID followingId = userProfileService.getProfile(username).getKeycloakId();
        return userFollowService.getFollowers(followingId).stream()
                .map(userProfileMapper::toResponse)
                .toList();
    }

    @GetMapping("/{username}/following")
    public List<UserProfileResponse> getFollowing(@PathVariable String username) {
        UUID followerId = userProfileService.getProfile(username).getKeycloakId();
        return userFollowService.getFollowing(followerId).stream()
                .map(userProfileMapper::toResponse)
                .toList();
    }
}
