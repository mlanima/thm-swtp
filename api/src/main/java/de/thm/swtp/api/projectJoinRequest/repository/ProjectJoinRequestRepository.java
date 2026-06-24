package de.thm.swtp.api.projectJoinRequest.repository;

import de.thm.swtp.api.projectJoinRequest.domain.ProjectJoinRequestStatus;
import de.thm.swtp.api.projectJoinRequest.entity.ProjectJoinRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Repository for join-requests to projects. */
public interface ProjectJoinRequestRepository extends JpaRepository<ProjectJoinRequestEntity, UUID> {

    /** Checks if there is already a join-request to a given project from a given user with the given request-status. */
    boolean existsByProjectIdAndRequestingUserKeycloakIdAndStatusIn(UUID projectId, UUID requestingUserKeyCloakId, Collection<ProjectJoinRequestStatus> statuses);

    /** Returns a list of all join-requests to a given project. */
    List<ProjectJoinRequestEntity> findByProjectId(UUID projectId);

    /** Returns a list of all join-requests from a given user. */
    List<ProjectJoinRequestEntity> findByRequestingUserKeycloakId(UUID requestingUserKeycloakId);

    /** Checks if a join-requests exists and belongs to a project owned by the specified user.*/
    boolean existsByIdAndProjectOwnerKeycloakId(UUID requestId, UUID ownerId);

    /** Deletes all join-requests for a given project. */
    void deleteByProjectId(UUID projectId);
}
