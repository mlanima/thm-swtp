package de.thm.swtp.api.professorRequest.repository;

import de.thm.swtp.api.professorRequest.entity.ProfessorRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Repository for professor-rights requests. */
public interface ProfessorRequestRepository extends JpaRepository<ProfessorRequestEntity, UUID> {

    /** Returns a page of all requests ordered by creation date descending. */
    Page<ProfessorRequestEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
