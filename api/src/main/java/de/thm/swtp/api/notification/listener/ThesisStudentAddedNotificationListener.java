package de.thm.swtp.api.notification.listener;

import de.thm.swtp.api.notification.event.ThesisStudentAddedEvent;
import de.thm.swtp.api.thesis.notification.ThesisAssignmentNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ThesisStudentAddedNotificationListener {

    private final ThesisAssignmentNotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onThesisStudentAdded(ThesisStudentAddedEvent event) {
        log.debug("Thesis student added event received: thesis={}, student={}",
                event.thesisId(), event.studentKeycloakId());
        notificationService.createForStudentAdded(event.thesisId(), event.studentKeycloakId());
    }
}