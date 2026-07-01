package de.thm.swtp.api.professorRequest.repository;

import de.thm.swtp.api.professorRequest.domain.ProfessorRequestStatus;
import de.thm.swtp.api.professorRequest.entity.ProfessorRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository for professor-rights requests. */
public interface ProfessorRequestRepository extends JpaRepository<ProfessorRequestEntity, UUID> {

    /** Returns a page of all requests ordered by creation date descending. */
    Page<ProfessorRequestEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Returns true if the user already has a pending professor-rights request. */
    boolean existsByRequestingUserKeycloakIdAndStatus(UUID keycloakId, ProfessorRequestStatus status);

    /** Returns all requests for the given user, ordered by creation date descending. */
    List<ProfessorRequestEntity> findAllByRequestingUserKeycloakIdOrderByCreatedAtDesc(UUID keycloakId);

    Optional<ProfessorRequestEntity> findByVerificationTokenHash(String verificationTokenHash);

    boolean existsByRequestingUserKeycloakIdAndStatusIn(UUID keycloakId, List<ProfessorRequestStatus> statuses);
}
