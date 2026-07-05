package de.thm.swtp.api.userprofile.controller;

import de.thm.swtp.api.common.PageResponse;
import de.thm.swtp.api.userprofile.domain.UserStatus;
import de.thm.swtp.api.userprofile.dto.BanUserRequest;
import de.thm.swtp.api.userprofile.dto.ManagedUserResponse;
import de.thm.swtp.api.userprofile.mapper.ManagedUserMapper;
import de.thm.swtp.api.userprofile.service.UserProfileService;
import de.thm.swtp.api.auditlog.AuditActor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/management")
public class UserManagementController {
    private final UserProfileService userProfileService;
    private final ManagedUserMapper managedUserMapper;

    @GetMapping
    @PreAuthorize("@security.canViewManagedUsers(authentication)")
    public PageResponse<ManagedUserResponse> getUsers(@RequestParam(name = "status", defaultValue = "ACTIVE") UserStatus userStatus,
                                                      @PageableDefault(size = 20, sort = "username", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<ManagedUserResponse> users = userProfileService.getUsersByStatus(userStatus, pageable)
                .map(managedUserMapper::toResponse);

        return PageResponse.toResponse(users);
    }

    @PatchMapping("/{userId}/ban")
    @PreAuthorize("@security.canBanUser(#userId, authentication)")
    public ManagedUserResponse banUser(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody(required = false) BanUserRequest banUserRequest) {
        AuditActor actor = AuditActor.fromJwt(jwt);
        String banReason = banUserRequest == null ? null : banUserRequest.reason();
        return managedUserMapper.toResponse(userProfileService.banUser(userId, banReason, actor));
    }

    @PatchMapping("/{userId}/unban")
    @PreAuthorize("@security.canUnbanUser(#userId, authentication)")
    public ManagedUserResponse unbanUser(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt) {
        AuditActor actor = AuditActor.fromJwt(jwt);
        return managedUserMapper.toResponse(userProfileService.unbanUser(userId, actor));
    }

}
