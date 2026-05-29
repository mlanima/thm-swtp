package de.thm.swtp.api.projectJoinRequest.service;

import de.thm.swtp.api.exceptionhandling.exceptions.ProjectJoinRequestAlreadyExistsException;
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

import java.util.EnumSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectJoinRequestService {
    private final ProjectJoinRequestRepository projectJoinRequestRepository;
    private final ProjectRepository projectRepository;
    private final UserProfileRepository userProfileRepository;


    public ProjectJoinRequest createProjectJoinRequest(UUID projectId, UUID currentUserId, String message){
        ProjectEntity projectEntity = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        UserProfile requestingUserEntity = userProfileRepository.findById(currentUserId)
                .orElseThrow(() -> new UserProfileNotFoundException(currentUserId.toString()));

        if(hasActiveRequests(projectId,currentUserId)){
            throw new ProjectJoinRequestAlreadyExistsException("A join request has already been created for the project: " + projectId);
        }

        ProjectJoinRequestEntity joinRequestEntity = ProjectJoinRequestEntity.builder()
                .project(projectEntity)
                .requestingUser(requestingUserEntity)
                .message(message)
                .build();

        ProjectJoinRequestEntity saved =  projectJoinRequestRepository.save(joinRequestEntity);
        return ProjectJoinRequestMapper.toDomain(saved);
    }


    private boolean hasActiveRequests(UUID projectId, UUID currentUserId) {
        EnumSet<ProjectJoinRequestStatus> activeRequestStates = EnumSet.of(ProjectJoinRequestStatus.PENDING, ProjectJoinRequestStatus.ACCEPTED);
        return projectJoinRequestRepository.existsByProjectIdAndRequestingUserKeycloakIdAndStatusIn(projectId, currentUserId, activeRequestStates);

    }
}
