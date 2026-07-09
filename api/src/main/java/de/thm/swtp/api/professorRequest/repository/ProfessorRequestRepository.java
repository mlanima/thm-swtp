package de.thm.swtp.api.professorRequest.repository;

import de.thm.swtp.api.professorRequest.domain.ProfessorRequestStatus;
import de.thm.swtp.api.professorRequest.entity.ProfessorRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository for professor-rights requests. */
public interface ProfessorRequestRepository extends JpaRepository<ProfessorRequestEntity, UUID> {

    /** Returns a page of all requests ordered by creation date descending. */
    Page<ProfessorRequestEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Returns all requests for the given user, ordered by creation date descending. */
    List<ProfessorRequestEntity> findAllByRequestingUserKeycloakIdOrderByCreatedAtDesc(UUID keycloakId);

    /** Returns a professor request by its hashed email-verification token.*/
    Optional<ProfessorRequestEntity> findByVerificationTokenHash(String verificationTokenHash);

    /** Checks if a user already has a professor requests in one of the given statuses.*/
    boolean existsByRequestingUserKeycloakIdAndStatusIn(UUID keycloakId, List<ProfessorRequestStatus> statuses);

    /** Returns all professor requests in the given status whose email-verification is expired.*/
    List<ProfessorRequestEntity> findAllByStatusAndVerificationExpiresAtBefore(ProfessorRequestStatus status, LocalDateTime now);

    /** Returns all professor requests of a user in the given status whose email-verification is expired.*/
    List<ProfessorRequestEntity> findAllByRequestingUserKeycloakIdAndStatusAndVerificationExpiresAtBefore(UUID keycloakId, ProfessorRequestStatus status, LocalDateTime now);
}
