package de.thm.swtp.api.userprofile.controller;

import de.thm.swtp.api.common.PageResponse;
import de.thm.swtp.api.userprofile.domain.UserStatus;
import de.thm.swtp.api.userprofile.dto.BanUserRequest;
import de.thm.swtp.api.userprofile.dto.UserProfileResponse;
import de.thm.swtp.api.userprofile.mapper.UserProfileMapper;
import de.thm.swtp.api.userprofile.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/management")
public class UserManagementController {
    private final UserProfileService userProfileService;
    private final UserProfileMapper userProfileMapper;

    @GetMapping
    @PreAuthorize("@security.canViewManagedUsers(authentication)")
    public PageResponse<UserProfileResponse> getUsers(@RequestParam(name = "status", defaultValue = "ACTIVE") UserStatus userStatus,
                                                      @PageableDefault(size = 20, sort = "username", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<UserProfileResponse> users = userProfileService.getUsersByStatus(userStatus, pageable)
                .map(userProfileMapper::toResponse);

        return PageResponse.toResponse(users);
    }

    @PatchMapping("/{userId}/ban")
    @PreAuthorize("@security.canBanUser(#userId, authentication)")
    public UserProfileResponse banUser(@PathVariable UUID userId, @Valid @RequestBody(required = false) BanUserRequest banUserRequest) {
        String banReason = banUserRequest == null ? null : banUserRequest.reason();
        return userProfileMapper.toResponse(userProfileService.banUser(userId, banReason));
    }

    @PatchMapping("/{userId}/unban")
    @PreAuthorize("@security.canUnbanUser(#userId, authentication)")
    public UserProfileResponse unbanUser(@PathVariable UUID userId){
        return userProfileMapper.toResponse(userProfileService.unbanUser(userId));
    }

}
