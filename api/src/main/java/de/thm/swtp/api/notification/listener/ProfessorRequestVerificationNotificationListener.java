package de.thm.swtp.api.notification.listener;

import de.thm.swtp.api.notification.event.ProfessorRequestVerificationCreatedEvent;
import de.thm.swtp.api.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfessorRequestVerificationNotificationListener {
    private final NotificationService notificationService;

    @EventListener
    public void onProfessorRequestVerificationCreated(ProfessorRequestVerificationCreatedEvent event) {
        notificationService.sendProfessorRequestVerificationMail(event);
    }
}
