package de.thm.swtp.api.projectJoinRequest.service;

import de.thm.swtp.api.exceptionhandling.exceptions.ProjectJoinRequestAccessDeniedException;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectJoinRequestAlreadyExistsException;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectJoinRequestInvalidStatusForEditException;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectJoinRequestNotFoundException;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import de.thm.swtp.api.projectJoinRequest.domain.ProjectJoinRequest;
import de.thm.swtp.api.projectJoinRequest.domain.ProjectJoinRequestStatus;
import de.thm.swtp.api.projectJoinRequest.entity.ProjectJoinRequestEntity;
import de.thm.swtp.api.projectJoinRequest.mapper.ProjectJoinRequestMapper;
import de.thm.swtp.api.projectJoinRequest.repository.ProjectJoinRequestRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/** Service for managing project join-requests. */
@Service
@RequiredArgsConstructor
public class ProjectJoinRequestService {
    private final ProjectJoinRequestRepository projectJoinRequestRepository;
    private final ProjectRepository projectRepository;
    private final UserProfileRepository userProfileRepository;


    /** Creates a new join-request for the given project and from the given user.
     *  Join-request can only be created when the project and the user exist and
     *  when there are no further active join requests for the same project and user.
     */
    @Transactional
    public ProjectJoinRequest createProjectJoinRequest(UUID projectId, UUID currentUserId, String message){
        ProjectEntity projectEntity = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        UserProfile requestingUserEntity = userProfileRepository.findById(currentUserId)
                .orElseThrow(() -> new UserProfileNotFoundException(currentUserId.toString()));

        if(hasActiveRequests(projectId,currentUserId)){
            throw new ProjectJoinRequestAlreadyExistsException(projectId);
        }

        ProjectJoinRequestEntity joinRequestEntity = ProjectJoinRequestEntity.builder()
                .project(projectEntity)
                .requestingUser(requestingUserEntity)
                .message(message)
                .build();

        ProjectJoinRequestEntity saved =  projectJoinRequestRepository.save(joinRequestEntity);
        return ProjectJoinRequestMapper.toDomain(saved);
    }

    /** Updates the status of a project join-request to accepted.
     * Only possible for the project owner.
     * The user, who created the join-request, is added as a member of the given project. */
    @Transactional
    public ProjectJoinRequest acceptProjectJoinRequest(UUID requestId, UUID currentUserId){
        ProjectJoinRequestEntity joinRequestEntity = projectJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ProjectJoinRequestNotFoundException(requestId));

        ProjectEntity projectEntity = joinRequestEntity.getProject();

        checkProjectOwner(projectEntity, currentUserId);

        if(joinRequestEntity.getStatus() !=  ProjectJoinRequestStatus.PENDING){
            throw new ProjectJoinRequestInvalidStatusForEditException("Only a pending join-request can be accepted.");
        }

        joinRequestEntity.setStatus(ProjectJoinRequestStatus.ACCEPTED);

        projectEntity.getMembers().add(joinRequestEntity.getRequestingUser());
        ProjectJoinRequestEntity saved = projectJoinRequestRepository.save(joinRequestEntity);

        return ProjectJoinRequestMapper.toDomain(saved);

    }

    /** Updates the status of a project join-request to rejected.
     * Only possible for the project owner
     * The user, who created the join-request, is removed as a member  of the given project. */
    @Transactional
    public ProjectJoinRequest rejectProjectJoinRequest(UUID requestId, UUID currentUserId){
        ProjectJoinRequestEntity joinRequestEntity = projectJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ProjectJoinRequestNotFoundException(requestId));

        ProjectEntity projectEntity = joinRequestEntity.getProject();

        checkProjectOwner(projectEntity, currentUserId);

        if(joinRequestEntity.getStatus() !=  ProjectJoinRequestStatus.PENDING){
            throw new ProjectJoinRequestInvalidStatusForEditException("Only a pending join-request can be rejected.");
        }

        joinRequestEntity.setStatus(ProjectJoinRequestStatus.REJECTED);
        ProjectJoinRequestEntity saved = projectJoinRequestRepository.save(joinRequestEntity);

        return ProjectJoinRequestMapper.toDomain(saved);
    }


    /** Returns all join-requests for a given project.
     * Only the project owner is allowed to get the join-requests for his project. */
    @Transactional
    public List<ProjectJoinRequest> getProjectJoinRequestsForProject(UUID projectId, UUID currentUserId){
        ProjectEntity projectEntity = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        checkProjectOwner(projectEntity, currentUserId);

        return projectJoinRequestRepository.findByProjectId(projectId)
                .stream()
                .map(ProjectJoinRequestMapper::toDomain)
                .toList();
    }

    /** Returns all join-requests sent by the given user.*/
    @Transactional
    public List<ProjectJoinRequest> getProjectJoinRequestsFromUser(UUID currentUserId){
        return projectJoinRequestRepository.findByRequestingUserKeycloakId(currentUserId)
                .stream()
                .map(ProjectJoinRequestMapper::toDomain)
                .toList();
    }


    private boolean hasActiveRequests(UUID projectId, UUID currentUserId) {
        EnumSet<ProjectJoinRequestStatus> activeRequestStates = EnumSet.of(ProjectJoinRequestStatus.PENDING, ProjectJoinRequestStatus.ACCEPTED);
        return projectJoinRequestRepository.existsByProjectIdAndRequestingUserKeycloakIdAndStatusIn(projectId, currentUserId, activeRequestStates);

    }

    private void checkProjectOwner(ProjectEntity projectEntity, UUID currentUserId) {
        UUID ownerId = projectEntity.getOwner().getKeycloakId();

        if(!ownerId.equals(currentUserId)){
            throw new ProjectJoinRequestAccessDeniedException("Only the project owner is allowed to manage join-requests.");
        }
    }
}
