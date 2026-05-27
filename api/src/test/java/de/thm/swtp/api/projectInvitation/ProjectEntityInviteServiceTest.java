package de.thm.swtp.api.projectInvitation;

import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import de.thm.swtp.api.projectInvitation.domain.ProjectInvite;
import de.thm.swtp.api.projectInvitation.domain.ProjectInviteStatus;
import de.thm.swtp.api.projectInvitation.entity.ProjectInviteEntity;
import de.thm.swtp.api.projectInvitation.exception.InvalidProjectInviteException;
import de.thm.swtp.api.projectInvitation.exception.ProjectInviteAccessDeniedException;
import de.thm.swtp.api.projectInvitation.exception.ProjectInviteNotFoundException;
import de.thm.swtp.api.projectInvitation.repository.ProjectInviteRepository;
import de.thm.swtp.api.projectInvitation.service.ProjectInviteService;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ProjectEntityInviteServiceTest {

    private ProjectInviteRepository projectInviteRepository;
    private ProjectRepository projectRepository;
    private UserProfileRepository userProfileRepository;

    private ProjectInviteService projectInviteService;

    private UUID projectId;
    private UUID ownerId;
    private UUID invitedUserId;
    private UUID inviteId;

    private ProjectEntity project;
    private UserProfile owner;
    private UserProfile invitedUser;

    @BeforeEach
    void setup() {
        projectInviteRepository = mock(ProjectInviteRepository.class);
        projectRepository = mock(ProjectRepository.class);
        userProfileRepository = mock(UserProfileRepository.class);

        projectInviteService = new ProjectInviteService(projectInviteRepository, projectRepository, userProfileRepository);

        projectId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        invitedUserId = UUID.randomUUID();
        inviteId = UUID.randomUUID();

        owner = new UserProfile();
        owner.setKeycloakId(ownerId);

        invitedUser = new UserProfile();
        invitedUser.setKeycloakId(invitedUserId);

        project = new ProjectEntity();
        project.setId(projectId);
        project.setOwner(owner);
    }

    @Test
    void getInvitesForUser_shouldReturnInvitesForUser(){
        ProjectInviteEntity invite = createPendingInviteEntity();

        when(projectInviteRepository.findByInvitedUserKeycloakId(invitedUserId)).thenReturn(List.of(invite));

        List<ProjectInvite> res =  projectInviteService.getInvitesForUser(invitedUserId);

        assertThat(res).hasSize(1);
        assertThat(res.getFirst().getInvitedUserId()).isEqualTo(invitedUserId);
        assertThat(res.getFirst().getProjectId()).isEqualTo(projectId);
    }

    @Test
    void createProjectInvite_shouldCreateInvite_whenSenderIsProjectOwner(){
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userProfileRepository.findById(invitedUserId)).thenReturn(Optional.of(invitedUser));
        when(projectInviteRepository.findByProjectIdAndInvitedUserKeycloakIdAndStatus(projectId,invitedUserId, ProjectInviteStatus.PENDING)).thenReturn(Optional.empty());

        when(projectInviteRepository.save(any(ProjectInviteEntity.class))).thenAnswer(invocation -> {
            ProjectInviteEntity entity = invocation.getArgument(0);
            entity.setId(inviteId);
            entity.setCreatedAt(LocalDateTime.now());
            return entity;
        });

        ProjectInvite res = projectInviteService.createProjectInvite(projectId,invitedUserId, "Hello, please join my project.", ownerId);

        assertThat(res.getId()).isEqualTo(inviteId);
        assertThat(res.getProjectId()).isEqualTo(projectId);
        assertThat(res.getInvitedUserId()).isEqualTo(invitedUserId);
        assertThat(res.getMessage()).isEqualTo("Hello, please join my project.");
        assertThat(res.getStatus()).isEqualTo(ProjectInviteStatus.PENDING);
        assertThat(res.getCreatedAt()).isNotNull();

        ArgumentCaptor<ProjectInviteEntity> captor = ArgumentCaptor.forClass(ProjectInviteEntity.class);
        verify(projectInviteRepository).save(captor.capture());

        ProjectInviteEntity saved =  captor.getValue();
        assertThat(saved.getProject()).isEqualTo(project);
        assertThat(saved.getInvitedUser()).isEqualTo(invitedUser);
        assertThat(saved.getStatus()).isEqualTo(ProjectInviteStatus.PENDING);
    }

    @Test
    void createProjectInvite_shouldThrow_whenSenderIsNotProjectOwner(){
        UUID otherUserId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userProfileRepository.findById(invitedUserId)).thenReturn(Optional.of(invitedUser));

        assertThatThrownBy(() -> projectInviteService.createProjectInvite(projectId,invitedUserId, "Hello, please join my project.", otherUserId))
                .isInstanceOf(ProjectInviteAccessDeniedException.class)
                .hasMessage("Only the project owner is allowed to create invitations.");

        verify(projectInviteRepository, never()).save(any());
    }


    @Test
    void createProjectInvite_shouldThrow_whenOwnerInvitesHimself() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userProfileRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> projectInviteService.createProjectInvite(
                projectId,
                ownerId,
                "Hello, please join my project.",
                ownerId
        ))
                .isInstanceOf(InvalidProjectInviteException.class)
                .hasMessage("The project owner cannot invite himself to the project.");

        verify(projectInviteRepository, never()).save(any());
    }

    @Test
    void createProjectInvite_shouldThrow_whenPendingInviteExists(){
        ProjectInviteEntity existingInvite = new ProjectInviteEntity();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userProfileRepository.findById(invitedUserId)).thenReturn(Optional.of(invitedUser));
        when(projectInviteRepository.findByProjectIdAndInvitedUserKeycloakIdAndStatus(projectId,invitedUserId, ProjectInviteStatus.PENDING))
                .thenReturn(Optional.of(existingInvite));

        assertThatThrownBy(() -> projectInviteService.createProjectInvite(projectId,invitedUserId, "Hello, please join my project.", ownerId))
                .isInstanceOf(InvalidProjectInviteException.class)
                .hasMessage("User already has a pending invitation for this project.");

        verify(projectInviteRepository, never()).save(any());
    }

    @Test
    void createProjectInvite_shouldThrow_whenProjectDoesNotExist(){
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectInviteService.createProjectInvite(projectId,invitedUserId, "Hello, please join my project.", ownerId))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessage("Project not found: " + projectId);

        verify(projectInviteRepository, never()).save(any());
    }

    @Test
    void createProjectInvite_shouldThrow_whenInvitedUserDoesNotExist(){
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userProfileRepository.findById(invitedUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(()-> projectInviteService.createProjectInvite(projectId,invitedUserId, "Hello, please join my project.", ownerId))
                .isInstanceOf(UserProfileNotFoundException.class)
                .hasMessage("Profile not found for user: " + invitedUserId);

        verify(projectInviteRepository, never()).save(any());
    }

    @Test
    void updateInviteStatus_shouldThrow_whenInviteDoesNotExist(){
        when(projectInviteRepository.findById(inviteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectInviteService.updateInviteStatus(inviteId,ProjectInviteStatus.ACCEPTED,invitedUserId))
                .isInstanceOf(ProjectInviteNotFoundException.class)
                .hasMessage("Project invite not found: " + inviteId);
    }

    @Test
    void updateInviteStatus_shouldAcceptInvite_whenCurrentUserIsInvitedUser(){
        ProjectInviteEntity invite = createPendingInviteEntity();

        when(projectInviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));

        ProjectInvite res = projectInviteService.updateInviteStatus(inviteId,ProjectInviteStatus.ACCEPTED, invitedUserId);

        assertThat(res.getStatus()).isEqualTo(ProjectInviteStatus.ACCEPTED);
        assertThat(invite.getStatus()).isEqualTo(ProjectInviteStatus.ACCEPTED);
    }

    @Test
    void updateInviteStatus_shouldRejectInvite_whenCurrentUserIsInvitedUser(){
        ProjectInviteEntity invite = createPendingInviteEntity();
        when(projectInviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));

        ProjectInvite res = projectInviteService.updateInviteStatus(inviteId,ProjectInviteStatus.REJECTED, invitedUserId);

        assertThat(res.getStatus()).isEqualTo(ProjectInviteStatus.REJECTED);
        assertThat(invite.getStatus()).isEqualTo(ProjectInviteStatus.REJECTED);
    }



    @Test
    void updateInviteStatus_shouldThrow_whenCurrentUserIsNotInvitedUser(){
        ProjectInviteEntity invite = createPendingInviteEntity();
        UUID otherUserId = UUID.randomUUID();

        when(projectInviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> projectInviteService.updateInviteStatus(inviteId,ProjectInviteStatus.ACCEPTED, otherUserId))
                .isInstanceOf(ProjectInviteAccessDeniedException.class)
                .hasMessage("Only the invited user can update the invitation status.");
    }

    @Test
    void updateInviteStatus_shouldThrow_whenNewStatusIsPending(){
        ProjectInviteEntity invite = createPendingInviteEntity();

        when(projectInviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> projectInviteService.updateInviteStatus(inviteId,ProjectInviteStatus.PENDING, invitedUserId))
                .isInstanceOf(InvalidProjectInviteException.class)
                .hasMessage("Invalid invitation status.");
    }

    @Test
    void updateInviteStatus_shouldThrow_whenInviteIsAlreadyAccepted(){
        ProjectInviteEntity invite = createPendingInviteEntity();
        invite.setStatus(ProjectInviteStatus.ACCEPTED);

        when(projectInviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> projectInviteService.updateInviteStatus(inviteId,ProjectInviteStatus.REJECTED,invitedUserId))
                .isInstanceOf(InvalidProjectInviteException.class)
                .hasMessage("Only invitations that are pending can be updated.");
    }



    private ProjectInviteEntity createPendingInviteEntity() {
        ProjectInviteEntity invite = new ProjectInviteEntity();
        invite.setId(inviteId);
        invite.setProject(project);
        invite.setInvitedUser(invitedUser);
        invite.setMessage("Hello, please join my project.");
        invite.setCreatedAt(LocalDateTime.now());
        invite.setStatus(ProjectInviteStatus.PENDING);

        return invite;
    }

}



