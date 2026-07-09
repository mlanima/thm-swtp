package de.thm.swtp.api.professorRequest.scheduler;

import de.thm.swtp.api.professorRequest.service.ProfessorRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProfessorRequestExpirationScheduler {
    private final ProfessorRequestService professorRequestService;

    @Scheduled(fixedDelayString = "${app.professor-request.expiration-check-delay-ms:3600000}")
    public void expireOutdatedVerificationRequests() {
        int expiredCount = professorRequestService.expireOutdatedVerificationRequests();

        if (expiredCount > 0) {
            log.info("Expired professor request email verifications: count={}", expiredCount);
        }
    }
}
