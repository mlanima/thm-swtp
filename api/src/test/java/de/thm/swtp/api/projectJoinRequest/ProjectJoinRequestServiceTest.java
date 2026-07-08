package de.thm.swtp.api.projectJoinRequest;

import de.thm.swtp.api.exceptionhandling.exceptions.ProjectJoinRequestAlreadyExistsException;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectJoinRequestInvalidStatusForEditException;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectJoinRequestNotFoundException;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.projectJoinRequest.domain.ProjectJoinRequest;
import de.thm.swtp.api.projectJoinRequest.domain.ProjectJoinRequestStatus;
import de.thm.swtp.api.projectJoinRequest.entity.ProjectJoinRequestEntity;
import de.thm.swtp.api.projectJoinRequest.repository.ProjectJoinRequestRepository;
import de.thm.swtp.api.projectJoinRequest.service.ProjectJoinRequestService;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


public class ProjectJoinRequestServiceTest {

    private UUID projectId;
    private UUID ownerId;
    private UUID requestingUserId;
    private UUID requestId;

    private ProjectEntity project;
    private UserProfile owner;
    private UserProfile requestingUser;


    private ProjectJoinRequestRepository projectJoinRequestRepository;
    private ProjectRepository projectRepository;
    private UserProfileRepository userProfileRepository;
    private ProjectJoinRequestService projectJoinRequestService;

    @BeforeEach
    void setUp() {
        projectJoinRequestRepository = mock(ProjectJoinRequestRepository.class);
        projectRepository = mock(ProjectRepository.class);
        userProfileRepository = mock(UserProfileRepository.class);

        projectJoinRequestService = new ProjectJoinRequestService(
                projectJoinRequestRepository,
                projectRepository,
                userProfileRepository
        );

        projectId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        requestingUserId = UUID.randomUUID();
        requestId = UUID.randomUUID();

        owner = new UserProfile();
        owner.setKeycloakId(ownerId);

        requestingUser = new UserProfile();
        requestingUser.setKeycloakId(requestingUserId);
        requestingUser.setUsername("testuser");

        project = new ProjectEntity();
        project.setId(projectId);
        project.setOwner(owner);
        project.setMembers(new HashSet<>());
    }

    String message = "Hi, I would like to join your project.";

    @Test
    void createProjectJoinRequest_shouldCreateRequest_whenProjectAndUserExistAndNoActiveRequestExists() {

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userProfileRepository.findById(requestingUserId)).thenReturn(Optional.of(requestingUser));
        when(projectJoinRequestRepository.existsByProjectIdAndRequestingUserKeycloakIdAndStatusIn(
                eq(projectId),
                eq(requestingUserId),
                eq(EnumSet.of(ProjectJoinRequestStatus.PENDING, ProjectJoinRequestStatus.ACCEPTED))
        )).thenReturn(false);

        when(projectJoinRequestRepository.save(any(ProjectJoinRequestEntity.class)))
                .thenAnswer(invocation -> {
                    ProjectJoinRequestEntity entity = invocation.getArgument(0);
                    entity.setId(requestId);
                    entity.setCreatedAt(LocalDateTime.now());
                    entity.setUpdatedAt(LocalDateTime.now());
                    entity.setStatus(ProjectJoinRequestStatus.PENDING);
                    return entity;
                });

        ProjectJoinRequest res = projectJoinRequestService.createProjectJoinRequest(projectId,requestingUserId,message);

        assertThat(res).isNotNull();
        assertThat(res.getId()).isEqualTo(requestId);
        assertThat(res.getProjectId()).isEqualTo(projectId);
        assertThat(res.getRequestingUserId()).isEqualTo(requestingUserId);
        assertThat(res.getMessage()).isEqualTo(message);
        assertThat(res.getStatus()).isEqualTo(ProjectJoinRequestStatus.PENDING);

        verify(projectJoinRequestRepository).save(any(ProjectJoinRequestEntity.class));
    }

    @Test
    void createProjectJoinRequest_shouldThrowException_whenActiveRequestExists(){
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userProfileRepository.findById(requestingUserId)).thenReturn(Optional.of(requestingUser));
        when(projectJoinRequestRepository.existsByProjectIdAndRequestingUserKeycloakIdAndStatusIn(
                eq(projectId),
                eq(requestingUserId),
                any()
        )).thenReturn(true);

        assertThatThrownBy(() -> projectJoinRequestService.createProjectJoinRequest(projectId,requestingUserId,message))
                .isInstanceOf(ProjectJoinRequestAlreadyExistsException.class);

        verify(projectJoinRequestRepository,never()).save(any());
    }

    @Test
    void acceptProjectJoinRequest_shouldSetStatusAcceptedAndAddUserAsMember_whenCurrentUserIsOwner(){
        ProjectJoinRequestEntity joinRequest = ProjectJoinRequestEntity.builder()
                .id(requestId)
                .project(project)
                .requestingUser(requestingUser)
                .message(message)
                .status(ProjectJoinRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectJoinRequestRepository.findById(requestId)).thenReturn(Optional.of(joinRequest));
        when(projectJoinRequestRepository.save(joinRequest)).thenReturn(joinRequest);

        ProjectJoinRequest res = projectJoinRequestService.acceptProjectJoinRequest(requestId);


        assertThat(res.getStatus()).isEqualTo(ProjectJoinRequestStatus.ACCEPTED);
        assertThat(project.getMembers()).contains(requestingUser);
        verify(projectJoinRequestRepository).save(joinRequest);
    }


    @Test
    void acceptProjectJoinRequest_shouldThrowException_whenRequestIsNotPending() {
        ProjectJoinRequestEntity joinRequest = ProjectJoinRequestEntity.builder()
                .id(requestId)
                .project(project)
                .requestingUser(requestingUser)
                .status(ProjectJoinRequestStatus.REJECTED)
                .build();

        when(projectJoinRequestRepository.findById(requestId)).thenReturn(Optional.of(joinRequest));

        assertThatThrownBy(() -> projectJoinRequestService.acceptProjectJoinRequest(requestId))
                .isInstanceOf(ProjectJoinRequestInvalidStatusForEditException.class);

        verify(projectJoinRequestRepository, never()).save(any());
    }

    @Test
    void rejectProjectJoinRequest_shouldSetStatusRejected_whenCurrentUserIsOwner() {
        ProjectJoinRequestEntity joinRequest = ProjectJoinRequestEntity.builder()
                .id(requestId)
                .project(project)
                .requestingUser(requestingUser)
                .message(message)
                .status(ProjectJoinRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectJoinRequestRepository.findById(requestId)).thenReturn(Optional.of(joinRequest));
        when(projectJoinRequestRepository.save(joinRequest)).thenReturn(joinRequest);

        ProjectJoinRequest result = projectJoinRequestService.rejectProjectJoinRequest(requestId);

        assertThat(result.getStatus()).isEqualTo(ProjectJoinRequestStatus.REJECTED);

        verify(projectJoinRequestRepository).save(joinRequest);
    }

    @Test
    void getProjectJoinRequestsForProject_shouldReturnRequests_whenProjectExists() {
        ProjectJoinRequestEntity joinRequest =
                ProjectJoinRequestEntity.builder()
                        .id(requestId)
                        .project(project)
                        .requestingUser(requestingUser)
                        .message(message)
                        .status(ProjectJoinRequestStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

        when(projectRepository.existsById(projectId))
                .thenReturn(true);

        when(projectJoinRequestRepository.findByProjectId(projectId))
                .thenReturn(List.of(joinRequest));

        List<ProjectJoinRequest> result =
                projectJoinRequestService
                        .getProjectJoinRequestsForProject(projectId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(requestId);
    }

    @Test
    void getProjectJoinRequestsFromUser_shouldReturnOwnRequests() {
        ProjectJoinRequestEntity joinRequest = ProjectJoinRequestEntity.builder()
                .id(requestId)
                .project(project)
                .requestingUser(requestingUser)
                .message(message)
                .status(ProjectJoinRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectJoinRequestRepository.findByRequestingUserKeycloakId(requestingUserId))
                .thenReturn(List.of(joinRequest));

        List<ProjectJoinRequest> result = projectJoinRequestService.getProjectJoinRequestsFromUser(requestingUserId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getRequestingUserId()).isEqualTo(requestingUserId);
    }

    @Test
    void acceptProjectJoinRequest_shouldThrowNotFound_whenRequestDoesNotExist() {
        when(projectJoinRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectJoinRequestService.acceptProjectJoinRequest(requestId))
                .isInstanceOf(ProjectJoinRequestNotFoundException.class);
    }
}
