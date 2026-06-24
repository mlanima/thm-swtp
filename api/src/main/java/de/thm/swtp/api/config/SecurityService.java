package de.thm.swtp.api.config;

import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.projectInvitation.repository.ProjectInviteRepository;
import de.thm.swtp.api.projectJoinRequest.repository.ProjectJoinRequestRepository;
import de.thm.swtp.api.projectPost.repository.ProjectPostRepository;
import de.thm.swtp.api.userprofile.domain.UserStatus;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Authorization service.
 * <p> Public methods are referenced by controller {@code @PreAuthorize} expressions.
 * Relationships (e.g. project ownership, user-profile owner) are resolved through repository checks.</p>
 *
 * <p>Authorization checks fail when permissions are missing.</p>
 */

@Component("security")
@RequiredArgsConstructor
public class SecurityService {

    private final ProjectRepository projectRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProjectInviteRepository projectInviteRepository;
    private final ProjectJoinRequestRepository projectJoinRequestRepository;
    private final ProjectPostRepository projectPostRepository;

    // Project permissions

    /** Allowed to see projects.*/
    public boolean canViewProject(UUID projectId, Authentication authentication) {
        if (!hasAuthenticationContext(projectId, authentication)) {
            return false;
        }
        if (hasModeratorRole(authentication)) {
            return true;
        }
        if (!isRegularUser(authentication)) {
            return false;
        }

        return isProjectContributor(projectId, authentication) || isPublicProject(projectId);
    }

    /** Allowed to see project by url.*/
    public boolean canViewProjectByUrl(String projectUrl, Authentication authentication) {
        if (!hasAuthenticationContext(projectUrl, authentication)) {
            return false;
        }
        return projectRepository.findByProjectUrl(projectUrl)
                .map(project -> canViewProject(project.getId(), authentication))
                .orElse(false);
    }

    /** Allowed to create projects.*/
    public boolean canCreateProject(Authentication authentication) {
        return isRegularUser(authentication);
    }

    /** Allowed to edit project information (e.g. privacy settings, allow join-requests, description, ...).*/
    public boolean canEditProject(UUID projectId, Authentication authentication) {
        return isRegularUser(authentication) &&  isProjectOwner(projectId, authentication);
    }

    /** Allowed to delete a project.*/
    public boolean canDeleteProject(UUID projectId, Authentication authentication) {
        if (!hasAuthenticationContext(projectId, authentication)) {
            return false;
        }
        if (hasModeratorRole(authentication)) {
            return true;
        }

        return isProjectOwner(projectId, authentication) && isRegularUser(authentication);
    }

    /** Allowed to see project members.*/
    public boolean canViewProjectMembers(UUID projectId, Authentication authentication) {
        return canViewProject(projectId, authentication);
    }

    /** Allowed to remove project members.
     * Only project owner is allowed to remove members.
     * Owner cannot remove himself.
     */
    public boolean canRemoveProjectMember(UUID projectId, UUID memberId, Authentication authentication) {
        if (!hasAuthenticationContext(projectId, authentication) || memberId == null || !isRegularUser(authentication)) {
            return false;
        }

        UUID currentUserId = getCurrentUserId(authentication);
        return isProjectOwner(projectId, authentication) && !currentUserId.equals(memberId);
    }

    // Project invitation permissions

    /** Allowed to create project-invitation.*/
    public boolean canCreateProjectInvitation(UUID projectId, Authentication authentication) {
        if (!hasAuthenticationContext(projectId, authentication)) {
            return false;
        }
        return isRegularUser(authentication) && isProjectOwner(projectId, authentication);
    }

    /** Allowed to see project-invitations.*/
    public boolean canViewProjectInvites(UUID projectId, Authentication authentication) {
        if (!hasAuthenticationContext(projectId, authentication)) {
            return false;
        }
        return isRegularUser(authentication) && isProjectOwner(projectId, authentication);
    }

    /** Allowed to accept/reject a project invite.*/
    public boolean canRespondToProjectInvite(UUID inviteId, Authentication authentication) {
        if (!hasAuthenticationContext(inviteId, authentication) || !isRegularUser(authentication)) {
            return false;
        }
        UUID currentUserId = getCurrentUserId(authentication);
        return projectInviteRepository.existsByIdAndInvitedUserKeycloakId(inviteId, currentUserId);
    }


    // Project link permissions

    public boolean canViewProjectLinks(UUID projectId, Authentication authentication) {
        return canViewProject(projectId, authentication);
    }

    public boolean canCreateProjectLink(UUID projectId, Authentication authentication) {
        return canEditProject(projectId, authentication);
    }

    public boolean canEditProjectLink(UUID projectId, Authentication authentication) {
        return canEditProject(projectId, authentication);
    }

    public boolean canDeleteProjectLink(UUID projectId, Authentication authentication) {
        return canEditProject(projectId, authentication);
    }

    // Project join-request permissions

    /** Allowed to see project join-requests.*/
    public boolean canViewProjectJoinRequests(UUID projectId, Authentication authentication) {
        if (!hasAuthenticationContext(projectId, authentication)) {
            return false;
        }
        return isProjectOwner(projectId, authentication) && isRegularUser(authentication);
    }

    /** Allowed to create project join-requests.*/
    public boolean canCreateProjectJoinRequest(UUID projectId, Authentication authentication) {
        if (!hasAuthenticationContext(projectId, authentication)) {
            return false;
        }
        return isRegularUser(authentication) && !isProjectContributor(projectId, authentication);
    }

    /** Allowed to accept/reject project join-requests.*/
    public boolean canManageProjectJoinRequests(UUID requestId, Authentication authentication) {
        if (!hasAuthenticationContext(requestId, authentication) || !isRegularUser(authentication)) {
            return false;
        }
        UUID currentUserId = getCurrentUserId(authentication);
        return projectJoinRequestRepository.existsByIdAndProjectOwnerKeycloakId(requestId, currentUserId);
    }


    // Project post permissions

    /** Allowed to create post on a project.*/
    public boolean canCreateProjectPost(UUID projectId, Authentication authentication) {
        if (!hasAuthenticationContext(projectId, authentication)) {
            return false;
        }
        return isProjectContributor(projectId, authentication);
    }

    /** Allowed to archive posts on a  project.*/
    public boolean canArchiveProjectPost(UUID projectId, UUID postId,  Authentication authentication) {
        if (!hasAuthenticationContext(projectId, authentication) || postId == null || !isRegularUser(authentication)) {
            return false;
        }
        return isProjectOwner(projectId, authentication) || isProjectPostAuthor(projectId, postId, authentication);
    }

    /** Allowed to publish posts on a  project.
     * Author is allowed to publish a draft.
     * Archived / published posts may be published by author / owner.
     */
    public boolean canPublishProjectPost(UUID projectId, UUID postId, Authentication authentication) {
        if (!hasAuthenticationContext(projectId, authentication) || postId == null || !isRegularUser(authentication)) {
            return false;
        }

        return projectPostRepository.findByIdAndProjectId(postId, projectId)
                .map(post -> switch (post.getStatus()){
                    case DRAFT -> isProjectPostAuthor(projectId, postId, authentication);
                    case ARCHIVED, PUBLISHED -> isProjectOwner(projectId, authentication) || isProjectPostAuthor(projectId, postId, authentication);
                })
                .orElse(false);
    }

    /** Allowed to delete a post on a  project.*/
    public boolean canDeleteProjectPost(UUID projectId, UUID postId, Authentication authentication) {
        if (!hasAuthenticationContext(projectId, authentication) || postId == null || !isRegularUser(authentication)) {
            return false;
        }
        return isProjectOwner(projectId, authentication) || isProjectPostAuthor(projectId, postId, authentication);
    }


    // Project favorite permissions

    /** Allowed to add a project as a favorite.*/
    public boolean canFavoriteProject(UUID projectId, Authentication authentication) {
        if (!hasAuthenticationContext(projectId, authentication)) {
            return false;
        }
        return isRegularUser(authentication) && canViewProject(projectId, authentication);
    }

    // Professor-request permissions

    /** Allowed to view all professor requests (moderator only). */
    public boolean canViewAllProfessorRequests(Authentication authentication) {
        return hasModeratorRole(authentication);
    }

    /** Allowed to view professor requests for a specific user (own or as moderator). */
    public boolean canViewProfessorRequestForUser(UUID userId, Authentication authentication) {
        if (!hasAuthenticationContext(userId, authentication)) {
            return false;
        }

        if (hasModeratorRole(authentication)){
            return true;
        }

        UUID currentUserId = getCurrentUserId(authentication);
        return isRegularUser(authentication) && userId.equals(currentUserId);
    }

    /** Allowed to manage (accept/reject) professor requests (moderator only). */
    public boolean canManageProfessorRequests(Authentication authentication) {
        return hasModeratorRole(authentication);
    }

    /** Allowed to create a professor request (regular users who are not already professors). */
    public boolean canCreateProfessorRequest(Authentication authentication) {
        if (!isRegularUser(authentication)) {
            return false;
        }
        UUID currentUserId = getCurrentUserId(authentication);
        return !userProfileRepository.existsByKeycloakIdAndProfessorTrue(currentUserId);
    }

    // User-profile permissions

    /** Allowed to view projects of a user.*/
    public boolean canViewUserProjects(String username, Authentication authentication) {
        if (!hasAuthenticationContext(username, authentication)) {
            return false;
        }
        return hasModeratorRole(authentication) || isProfileOwnerByUsername(username, authentication);
    }

    /** Allowed to edit the user profile.*/
    public boolean canEditUserProfile(String username, Authentication authentication) {
        if (!hasAuthenticationContext(username, authentication)) {
            return false;
        }
        return isProfileOwnerByUsername(username, authentication);
    }

    /** Allowed to delete the user profile. */
    public boolean canDeleteUserProfile(String username, Authentication authentication) {
        if (!hasAuthenticationContext(username, authentication)) {
            return false;
        }
       return hasModeratorRole(authentication) || isProfileOwnerByUsername(username, authentication);
    }



    // User-profile link permissions

    /** Allowed to create user-profile links.*/
    public boolean canCreateUserProfileLinks(UUID userId, Authentication authentication) {
       return isProfileOwnerByUserId(userId, authentication);
    }

    /** Allowed to edit user-profile links.*/
    public boolean canEditUserProfileLinks(UUID userId, Authentication authentication) {
        return isProfileOwnerByUserId(userId, authentication);
    }

    /** Allowed to delete user-profile links.*/
    public boolean canDeleteUserProfileLinks(UUID userId, Authentication authentication) {
        return isProfileOwnerByUserId(userId, authentication);
    }



    // User-management permissions

    /** Allowed to view users in the user-management page*/
    public boolean canViewManagedUsers(Authentication authentication) {
        return hasModeratorRole(authentication);
    }

    /** Allowed to ban users. Moderators cannot ban themselves.*/
    public boolean canBanUser(UUID userId, Authentication authentication) {
        if (!hasAuthenticationContext(userId, authentication) || !hasModeratorRole(authentication)) {
            return false;
        }

        UUID currentUserId = getCurrentUserId(authentication);
        if (currentUserId.equals(userId)) {
            return false;
        }

        return userProfileRepository.existsByKeycloakIdAndStatus(userId, UserStatus.ACTIVE);
    }

    /** Allowed to unban users.*/
    public boolean canUnbanUser(UUID userId, Authentication authentication) {
        if (!hasAuthenticationContext(userId, authentication) || !hasModeratorRole(authentication)) {
            return false;
        }

        return userProfileRepository.existsByKeycloakIdAndStatus(userId, UserStatus.BANNED);
    }



    // Authorization checks

    private UUID getCurrentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null || !authentication.isAuthenticated() || authority == null) {
            return false;
        }
        return authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority(authority));
    }

    private boolean hasAuthenticationContext(Object object, Authentication authentication) {
        return object != null && authentication != null && authentication.isAuthenticated();
    }

    public boolean hasModeratorRole(Authentication authentication) {
        return hasAuthority(authentication, "ROLE_MODERATOR");
    }

    private boolean hasUserRole(Authentication authentication) {
        return hasAuthority(authentication, "ROLE_USER");
    }

    private boolean isRegularUser(Authentication authentication) {
        return hasUserRole(authentication) && !hasModeratorRole(authentication) && isActiveUser(authentication);
    }

    private boolean isActiveUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        UUID currentUserId =  getCurrentUserId(authentication);
        return userProfileRepository.existsByKeycloakIdAndStatus(currentUserId, UserStatus.ACTIVE);
    }

    private boolean isProjectOwner(UUID projectId, Authentication authentication) {
        if (!hasAuthenticationContext(projectId, authentication)) {
            return false;
        }
        UUID currentUserId = getCurrentUserId(authentication);
        return projectRepository.existsByIdAndOwnerKeycloakId(projectId, currentUserId);
    }

    private boolean isProjectMember(UUID projectId, Authentication authentication) {
        if (!hasAuthenticationContext(projectId, authentication)) {
            return false;
        }
        UUID currentUserId = getCurrentUserId(authentication);
        return projectRepository.existsByIdAndMembersKeycloakId(projectId, currentUserId);
    }

    private boolean isProjectContributor(UUID projectId, Authentication authentication) {
        return isRegularUser(authentication) && (isProjectOwner(projectId, authentication) || isProjectMember(projectId, authentication));
    }

    private boolean isPublicProject(UUID projectId) {
        return projectRepository.existsByIdAndIsPrivateProjectFalse(projectId);
    }

    private boolean isProjectPostAuthor(UUID projectId, UUID postId, Authentication authentication) {
        if (!hasAuthenticationContext(projectId, authentication)) {
            return false;
        }
        UUID currentUserId = getCurrentUserId(authentication);
        return projectPostRepository.existsByIdAndProjectIdAndAuthorKeycloakId(postId, projectId, currentUserId);
    }


    private boolean isProfileOwnerByUsername(String username, Authentication authentication) {
        if (!hasAuthenticationContext(username, authentication) || !isRegularUser(authentication)) {
            return false;
        }
        UUID currentUserId = getCurrentUserId(authentication);
        return userProfileRepository.existsByUsernameAndKeycloakId(username, currentUserId);
    }

    private boolean isProfileOwnerByUserId(UUID userId, Authentication authentication) {
        if (!hasAuthenticationContext(userId, authentication) || !isRegularUser(authentication)) {
            return false;
        }
        UUID currentUserId = getCurrentUserId(authentication);
        return userId.equals(currentUserId);
    }

}

