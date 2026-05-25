package de.thm.swtp.api.projectInvitation.repository;

import de.thm.swtp.api.projectInvitation.entity.ProjectInviteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectInviteRepository extends JpaRepository<ProjectInviteEntity, UUID> {

    List<ProjectInviteEntity> findByProjectId(UUID projectId);
    // findByInvitedUserId missing, needs to be implemented when UserEntity is done
}
