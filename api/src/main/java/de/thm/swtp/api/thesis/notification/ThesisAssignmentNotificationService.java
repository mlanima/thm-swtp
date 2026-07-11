package de.thm.swtp.api.thesis.notification;

import de.thm.swtp.api.thesis.ThesisEntity;
import de.thm.swtp.api.thesis.ThesisRepository;
import de.thm.swtp.api.thesis.exception.ThesisNotFoundByIdException;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThesisAssignmentNotificationService {

    private final ThesisAssignmentNotificationRepository notificationRepository;
    private final ThesisRepository thesisRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public void createForStudentAdded(UUID thesisId, UUID studentKeycloakId) {
        ThesisEntity thesis = thesisRepository.findById(thesisId)
                .orElseThrow(() -> new ThesisNotFoundByIdException(thesisId));
        UserProfile student = userProfileRepository.findById(studentKeycloakId)
                .orElseThrow(() -> new UserProfileNotFoundException(studentKeycloakId.toString()));

        ThesisAssignmentNotificationEntity notification = ThesisAssignmentNotificationEntity.builder()
                .thesis(thesis)
                .student(student)
                .build();
        notificationRepository.save(notification);
        log.debug("Thesis assignment notification created: thesis={}, student={}", thesisId, studentKeycloakId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID studentKeycloakId) {
        return notificationRepository.countByStudentKeycloakIdAndReadFalse(studentKeycloakId);
    }

    @Transactional
    public void markAllAsRead(UUID studentKeycloakId) {
        List<ThesisAssignmentNotificationEntity> unread =
                notificationRepository.findByStudentKeycloakIdAndReadFalse(studentKeycloakId);
        unread.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unread);
    }
}