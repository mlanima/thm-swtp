package de.thm.swtp.api.notification.listener;

import de.thm.swtp.api.notification.event.ProjectInviteCreatedEvent;
import de.thm.swtp.api.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectInviteNotificationListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProjectInviteCreated(ProjectInviteCreatedEvent event) {
        try {
            notificationService.sendInviteMail(event);
        } catch (Exception e) {
            log.warn("Invite notification failed for {}: {}", event.invitedUserEmail(), e.getMessage());
        }
    }
}
