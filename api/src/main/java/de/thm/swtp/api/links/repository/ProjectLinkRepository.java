package de.thm.swtp.api.links.repository;

import de.thm.swtp.api.links.domain.LinkVisibility;
import de.thm.swtp.api.links.entity.ProjectLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectLinkRepository extends JpaRepository<ProjectLinkEntity, UUID> {

    List<ProjectLinkEntity> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

    List<ProjectLinkEntity> findByProjectIdAndVisibilityOrderByCreatedAtAsc(UUID projectId, LinkVisibility visibility);

    boolean existsByProjectIdAndUrlIgnoreCase(UUID projectId, String url);

}
