package de.thm.swtp.api.thesis.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ThesisAssignmentNotificationRepository
        extends JpaRepository<ThesisAssignmentNotificationEntity, UUID> {

    long countByStudentKeycloakIdAndReadFalse(UUID studentKeycloakId);

    List<ThesisAssignmentNotificationEntity> findByStudentKeycloakIdAndReadFalse(UUID studentKeycloakId);
}