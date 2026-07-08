package de.thm.swtp.api.notification.listener;

import de.thm.swtp.api.notification.event.ProfessorRequestVerificationCreatedEvent;
import de.thm.swtp.api.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ProfessorRequestVerificationNotificationListener {
    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProfessorRequestVerificationCreated(ProfessorRequestVerificationCreatedEvent event) {
        notificationService.sendProfessorRequestVerificationMail(event);
    }
}
