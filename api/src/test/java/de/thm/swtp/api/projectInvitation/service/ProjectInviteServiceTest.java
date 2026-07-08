package de.thm.swtp.api.projectInvitation.service;

import de.thm.swtp.api.notification.event.ProjectMemberAddedEvent;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.projectInvitation.domain.ProjectInviteStatus;
import de.thm.swtp.api.projectInvitation.entity.ProjectInviteEntity;
import de.thm.swtp.api.projectInvitation.mapper.ProjectInviteMapper;
import de.thm.swtp.api.projectInvitation.repository.ProjectInviteRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectInviteServiceTest {

    private ProjectInviteRepository projectInviteRepository;
    private ProjectRepository projectRepository;
    private UserProfileRepository userProfileRepository;
    private ApplicationEventPublisher eventPublisher;
    private ProjectInviteMapper projectInviteMapper;
    private ProjectInviteService service;

    private UUID projectId;
    private UUID ownerId;
    private UUID invitedUserId;
    private UUID inviteId;

    private ProjectEntity project;
    private UserProfile owner;
    private UserProfile invitedUser;

    @BeforeEach
    void setUp() {
        projectInviteRepository = mock(ProjectInviteRepository.class);
        projectRepository = mock(ProjectRepository.class);
        userProfileRepository = mock(UserProfileRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        projectInviteMapper = mock(ProjectInviteMapper.class);

        service = new ProjectInviteService(
                projectInviteRepository, projectRepository, userProfileRepository, eventPublisher, projectInviteMapper);

        projectId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        invitedUserId = UUID.randomUUID();
        inviteId = UUID.randomUUID();

        owner = new UserProfile();
        owner.setKeycloakId(ownerId);

        invitedUser = new UserProfile();
        invitedUser.setKeycloakId(invitedUserId);
        invitedUser.setUsername("invitee");

        project = new ProjectEntity();
        project.setId(projectId);
        project.setOwner(owner);
        project.setMembers(new HashSet<>());
    }

    private ProjectInviteEntity pendingInvite() {
        ProjectInviteEntity invite = new ProjectInviteEntity();
        invite.setId(inviteId);
        invite.setProject(project);
        invite.setInvitedUser(invitedUser);
        invite.setStatus(ProjectInviteStatus.PENDING);
        invite.setCreatedAt(LocalDateTime.now());
        invite.setUpdatedAt(LocalDateTime.now());
        return invite;
    }

    @Test
    void shouldGrantMembershipAndPublishEventWhenInviteAccepted() {
        ProjectInviteEntity invite = pendingInvite();
        when(projectInviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));
        when(projectInviteRepository.save(any(ProjectInviteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateInviteStatus(inviteId, ProjectInviteStatus.ACCEPTED);

        assertThat(project.getMembers()).contains(invitedUser);
        verify(projectRepository).save(project);
        verify(eventPublisher).publishEvent(new ProjectMemberAddedEvent(projectId, invitedUserId));
    }

    @Test
    void shouldNotPublishEventWhenUserIsAlreadyAMember() {
        project.getMembers().add(invitedUser);
        ProjectInviteEntity invite = pendingInvite();
        when(projectInviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));
        when(projectInviteRepository.save(any(ProjectInviteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateInviteStatus(inviteId, ProjectInviteStatus.ACCEPTED);

        verify(projectRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(ProjectMemberAddedEvent.class));
    }

    @Test
    void shouldNotPublishEventWhenInviteRejected() {
        ProjectInviteEntity invite = pendingInvite();
        when(projectInviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));
        when(projectInviteRepository.save(any(ProjectInviteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateInviteStatus(inviteId, ProjectInviteStatus.REJECTED);

        assertThat(project.getMembers()).doesNotContain(invitedUser);
        verify(eventPublisher, never()).publishEvent(any(ProjectMemberAddedEvent.class));
    }
}
