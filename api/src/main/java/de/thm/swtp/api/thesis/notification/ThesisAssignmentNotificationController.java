package de.thm.swtp.api.thesis.notification;

import de.thm.swtp.api.thesis.notification.dto.response.UnreadThesisNotificationCountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/thesis-notifications")
@RequiredArgsConstructor
public class ThesisAssignmentNotificationController {

    private final ThesisAssignmentNotificationService notificationService;

    @GetMapping("/unread-count")
    public UnreadThesisNotificationCountResponse getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        return new UnreadThesisNotificationCountResponse(notificationService.getUnreadCount(currentUserId));
    }

    @PostMapping("/mark-read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        notificationService.markAllAsRead(currentUserId);
    }
}
