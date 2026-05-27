package de.thm.swtp.api.projectInvitation.repository;

import de.thm.swtp.api.projectInvitation.domain.ProjectInviteStatus;
import de.thm.swtp.api.projectInvitation.entity.ProjectInviteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectInviteRepository extends JpaRepository<ProjectInviteEntity, UUID> {

    /** Returns a list of all invitations from a specific project.*/
    List<ProjectInviteEntity> findByProjectId(UUID projectId);
    /** Returns a list of all invitations send to a specific user.*/
    List<ProjectInviteEntity> findByInvitedUserKeycloakId(UUID invitedUserId);
    /** Returns all invites send to a specific user from a specific project with a specific status*/
    Optional<ProjectInviteEntity> findByProjectIdAndInvitedUserKeycloakIdAndStatus(UUID projectId, UUID invitedUserId, ProjectInviteStatus status);

}
