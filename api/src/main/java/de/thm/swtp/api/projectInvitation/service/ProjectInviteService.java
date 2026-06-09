package de.thm.swtp.api.projectInvitation.service;

import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import de.thm.swtp.api.projectInvitation.domain.ProjectInvite;
import de.thm.swtp.api.projectInvitation.domain.ProjectInviteStatus;
import de.thm.swtp.api.projectInvitation.entity.ProjectInviteEntity;
import de.thm.swtp.api.projectInvitation.exception.InvalidProjectInviteException;
import de.thm.swtp.api.projectInvitation.exception.ProjectInviteAccessDeniedException;
import de.thm.swtp.api.projectInvitation.exception.ProjectInviteNotFoundException;
import de.thm.swtp.api.projectInvitation.mapper.ProjectInviteMapper;
import de.thm.swtp.api.projectInvitation.repository.ProjectInviteRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** Service for managing {@link ProjectInvite} domain objects.*/
@Service
@RequiredArgsConstructor
public class ProjectInviteService {
    private final ProjectInviteRepository projectInviteRepository;
    private final ProjectRepository projectRepository;
    private final UserProfileRepository userProfileRepository;

    /** Creates a new project invitation. Only the project owner is allowed to send an invitation.*/
    @Transactional
    public ProjectInvite createProjectInvite(UUID projectId, UUID invitedUserId, String message, UUID senderId) {

        ProjectEntity projectEntity = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        UserProfile invitedUserEntity = userProfileRepository.findById(invitedUserId)
                .orElseThrow(() -> new UserProfileNotFoundException(invitedUserId.toString()));

        checkValidInviteCreation(projectEntity, invitedUserId, senderId);
        checkNoPendingInviteExists(projectId, invitedUserId);

        ProjectInviteEntity projectInviteEntity = createPendingInvite(projectEntity, invitedUserEntity, message);

        ProjectInviteEntity saved = projectInviteRepository.save(projectInviteEntity);
        return ProjectInviteMapper.toDomain(saved);
    }

    /** Returns all invitations that were sent to the given user.*/
    @Transactional(readOnly = true)
    public List<ProjectInvite> getInvitesForUser(UUID userId){
        return projectInviteRepository.findByInvitedUserKeycloakId(userId)
                .stream()
                .map(ProjectInviteMapper::toDomain)
                .toList();
    }

    /** Updates the status of the given invitation. Can only be updated when the status is on pending */
    @Transactional
    public ProjectInvite updateInviteStatus(UUID inviteId, ProjectInviteStatus newStatus, UUID currentUserId) {
        ProjectInviteEntity inviteEntity = projectInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ProjectInviteNotFoundException(inviteId));
        ProjectInvite invite = ProjectInviteMapper.toDomain(inviteEntity);

        checkInviteStatus(invite, newStatus, currentUserId);
        inviteEntity.setStatus(newStatus);


        if (newStatus == ProjectInviteStatus.ACCEPTED) {
            addInvitedUserToProject(inviteEntity);
        }

        ProjectInviteEntity saved = projectInviteRepository.save(inviteEntity);
        return ProjectInviteMapper.toDomain(saved);
    }





    private void checkValidInviteCreation(ProjectEntity projectEntity, UUID invitedUserId, UUID senderId) {
        UUID ownerId = projectEntity.getOwner().getKeycloakId();

        if (!ownerId.equals(senderId)) {
            throw new ProjectInviteAccessDeniedException("Only the project owner is allowed to create invitations.");
        }
        if (invitedUserId.equals(senderId)) {
            throw new InvalidProjectInviteException("The project owner cannot invite himself to the project.");
        }
    }

    private void checkNoPendingInviteExists(UUID projectId, UUID invitedUserId){
        boolean pendingInviteExists = projectInviteRepository
                .findByProjectIdAndInvitedUserKeycloakIdAndStatus(projectId, invitedUserId, ProjectInviteStatus.PENDING)
                .isPresent();
        if (pendingInviteExists) {
            throw new InvalidProjectInviteException("User already has a pending invitation for this project.");
        }
    }

    private void checkInviteStatus(ProjectInvite invite, ProjectInviteStatus newStatus, UUID currentUserId) {
        if (!invite.getInvitedUserId().equals(currentUserId)) {
            throw new ProjectInviteAccessDeniedException("Only the invited user can update the invitation status.");
        }
        if (invite.getStatus() != ProjectInviteStatus.PENDING) {
            throw new InvalidProjectInviteException("Only invitations that are pending can be updated.");
        }
        if (newStatus == null || !isFinalInviteStatus(newStatus)){
            throw new InvalidProjectInviteException("Invalid invitation status.");
        }
    }

    private boolean isFinalInviteStatus(ProjectInviteStatus status){
        return status == ProjectInviteStatus.ACCEPTED || status == ProjectInviteStatus.REJECTED;
    }

    private ProjectInviteEntity createPendingInvite(ProjectEntity projectEntity, UserProfile invitedUserEntity, String message){
        ProjectInviteEntity invite = new ProjectInviteEntity();
        invite.setProject(projectEntity);
        invite.setInvitedUser(invitedUserEntity);
        invite.setMessage(message);
        invite.setStatus(ProjectInviteStatus.PENDING);
        return invite;
    }

    private void addInvitedUserToProject(ProjectInviteEntity inviteEntity){
        ProjectEntity projectEntity = inviteEntity.getProject();
        UserProfile invitedUserEntity = inviteEntity.getInvitedUser();

        if (projectEntity.getOwner().getKeycloakId().equals(invitedUserEntity.getKeycloakId())) {
            return;
        }

        if (alreadyMember(projectEntity, invitedUserEntity)) {
            return;
        }
        projectEntity.getMembers().add(invitedUserEntity);
        projectRepository.save(projectEntity);
    }

    private boolean alreadyMember(ProjectEntity projectEntity, UserProfile invitedUserEntity){
        return projectEntity.getMembers()
                .stream()
                .anyMatch(member -> member.getKeycloakId().equals(invitedUserEntity.getKeycloakId()));
    }
}
