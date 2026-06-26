package de.thm.swtp.api.thesis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThesisRepository extends JpaRepository<ThesisEntity, UUID> {

    Optional<ThesisEntity> findByThesisUrl(String thesisUrl);

    boolean existsByTitle(String title);

    boolean existsByTitleAndIdNot(String title, UUID id);

    boolean existsByThesisUrl(String thesisUrl);

    boolean existsByThesisUrlAndIdNot(String thesisUrl, UUID id);

    boolean existsByThesisUrlAndSupervisorKeycloakId(String thesisUrl, UUID supervisorKeycloakId);

    boolean existsByIdAndSupervisorKeycloakId(UUID id, UUID supervisorKeycloakId);

    List<ThesisEntity> findBySupervisorUsername(String username);
}
