package de.thm.swtp.api.thesis;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThesisRepository extends JpaRepository<ThesisEntity, UUID> {

    @Query(value = "SELECT t FROM theses t JOIN FETCH t.supervisor",
           countQuery = "SELECT COUNT(t) FROM theses t")
    Page<ThesisEntity> findAll(Pageable pageable);

    @Query(value = "SELECT t FROM theses t JOIN FETCH t.supervisor WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :title, '%'))",
           countQuery = "SELECT COUNT(t) FROM theses t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    Page<ThesisEntity> findByTitleContainingIgnoreCase(@Param("title") String title, Pageable pageable);

    @Query("SELECT t FROM theses t JOIN FETCH t.supervisor WHERE t.supervisor.username = :username")
    List<ThesisEntity> findBySupervisorUsername(@Param("username") String username);

    Optional<ThesisEntity> findByThesisUrl(String thesisUrl);

    boolean existsByTitle(String title);

    boolean existsByTitleAndIdNot(String title, UUID id);

    boolean existsByThesisUrl(String thesisUrl);

    boolean existsByThesisUrlAndIdNot(String thesisUrl, UUID id);

    boolean existsByIdAndSupervisorKeycloakId(UUID id, UUID supervisorKeycloakId);
}
