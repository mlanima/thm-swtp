package de.thm.swtp.api.userprofile.controller;

import de.thm.swtp.api.userprofile.dto.UserProfileRequest;
import de.thm.swtp.api.userprofile.dto.UserProfileResponse;
import de.thm.swtp.api.userprofile.mapper.UserProfileMapper;
import de.thm.swtp.api.userprofile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserProfileMapper userProfileMapper;

    @GetMapping
    public UserProfileResponse getProfile(@PathVariable String userId) {
        return userProfileMapper.toResponse(userProfileService.getProfile(userId));
    }

    @PutMapping
    public UserProfileResponse updateProfile(
            @PathVariable String userId,
            @RequestBody UserProfileRequest request) {
        return userProfileMapper.toResponse(
                userProfileService.updateProfile(userId, request.about(), request.experience())
        );
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile(@PathVariable String userId) {
        userProfileService.deleteProfile(userId);
    }
}
