package de.thm.swtp.api.security;

import de.thm.swtp.api.config.SecurityService;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.projectInvitation.repository.ProjectInviteRepository;
import de.thm.swtp.api.projectJoinRequest.repository.ProjectJoinRequestRepository;
import de.thm.swtp.api.projectPost.domain.ProjectPostStatus;
import de.thm.swtp.api.projectPost.entity.ProjectPostEntity;
import de.thm.swtp.api.projectPost.repository.ProjectPostRepository;
import de.thm.swtp.api.thesis.ThesisRepository;
import de.thm.swtp.api.userprofile.domain.UserStatus;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private ProjectInviteRepository projectInviteRepository;
    @Mock
    private ProjectJoinRequestRepository projectJoinRequestRepository;
    @Mock
    private ProjectPostRepository projectPostRepository;
    @Mock
    private ThesisRepository thesisRepository;

    @InjectMocks
    private SecurityService securityService;

    private UUID userId;
    private UUID projectId;
    private UUID resourceId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        resourceId = UUID.randomUUID();
    }

    private Authentication authentication(String... extraRoles) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        for (String role : extraRoles) {
            authorities.add(new SimpleGrantedAuthority(role));
        }

        lenient().when(userProfileRepository.existsByKeycloakIdAndStatus(userId, UserStatus.ACTIVE))
                .thenReturn(true);

        return new UsernamePasswordAuthenticationToken(
                userId.toString(), null, authorities);
    }

    @Test
    void canViewProject_returnsTrue_forPublicProject() {
        when(projectRepository.existsByIdAndIsPrivateProjectFalse(projectId)).thenReturn(true);

        assertThat(securityService.canViewProject(projectId, authentication()))
                .isTrue();
    }

    @Test
    void canViewProject_returnsTrue_forProjectMember() {
        when(projectRepository.existsByIdAndMembersKeycloakId(projectId, userId)).thenReturn(true);

        assertThat(securityService.canViewProject(projectId, authentication()))
                .isTrue();
    }

    @Test
    void canViewProject_returnsTrue_forModerator() {
        assertThat(securityService.canViewProject(projectId, authentication("ROLE_MODERATOR")))
                .isTrue();
    }

    @Test
    void canViewProject_returnsFalse_forPrivateProjectAndStranger() {
        assertThat(securityService.canViewProject(projectId, authentication()))
                .isFalse();
    }

    @Test
    void canViewProject_returnsFalse_withoutAuthentication() {
        assertThat(securityService.canViewProject(projectId, null)).isFalse();
        verifyNoInteractions(projectRepository);
    }

    @Test
    void canViewProjectByUrl_delegatesToResolvedProject() {
        ProjectEntity project = new ProjectEntity();
        project.setId(projectId);
        when(projectRepository.findByProjectUrl("demo")).thenReturn(Optional.of(project));
        when(projectRepository.existsByIdAndIsPrivateProjectFalse(projectId)).thenReturn(true);

        assertThat(securityService.canViewProjectByUrl("demo", authentication()))
                .isTrue();
    }

    @Test
    void canCreateProject_allowsRegularUser_butNotModerator() {
        assertThat(securityService.canCreateProject(authentication())).isTrue();
        assertThat(securityService.canCreateProject(authentication("ROLE_MODERATOR"))).isFalse();
        assertThat(securityService.canCreateProject(authentication("ROLE_MODERATOR"))).isFalse();
    }

    @Test
    void canEditProject_allowsOwner() {
        when(projectRepository.existsByIdAndOwnerKeycloakId(projectId, userId)).thenReturn(true);

        assertThat(securityService.canEditProject(projectId, authentication()))
                .isTrue();
    }

    @Test
    void canEditProject_deniesNonOwner() {
        assertThat(securityService.canEditProject(projectId, authentication()))
                .isFalse();
    }

    @Test
    void canDeleteProject_allowsModerator() {
        assertThat(securityService.canDeleteProject(projectId, authentication("ROLE_MODERATOR")))
                .isTrue();
    }

    @Test
    void canViewProjectMembers_usesProjectVisibility() {
        when(projectRepository.existsByIdAndIsPrivateProjectFalse(projectId)).thenReturn(true);

        assertThat(securityService.canViewProjectMembers(projectId, authentication()))
                .isTrue();
    }

    @Test
    void canRemoveProjectMember_allowsOwnerRemovingAnotherUser() {
        UUID memberId = UUID.randomUUID();
        when(projectRepository.existsByIdAndOwnerKeycloakId(projectId, userId)).thenReturn(true);

        assertThat(securityService.canRemoveProjectMember(
                projectId, memberId, authentication())).isTrue();
    }

    @Test
    void canRemoveProjectMember_deniesRemovingSelf() {
        when(projectRepository.existsByIdAndOwnerKeycloakId(projectId, userId)).thenReturn(true);

        assertThat(securityService.canRemoveProjectMember(
                projectId, userId, authentication())).isFalse();
    }

    @Test
    void canCreateProjectInvitation_allowsProjectOwner() {
        when(projectRepository.existsByIdAndOwnerKeycloakId(projectId, userId)).thenReturn(true);

        assertThat(securityService.canCreateProjectInvitation(
                projectId, authentication())).isTrue();
    }


    @Test
    void canRespondToProjectInvite_checksInvitedUser() {
        when(projectInviteRepository.existsByIdAndInvitedUserKeycloakId(resourceId, userId))
                .thenReturn(true);

        assertThat(securityService.canRespondToProjectInvite(
                resourceId, authentication())).isTrue();
    }

    @Test
    void canViewProjectJoinRequests_allowsOwner() {
        when(projectRepository.existsByIdAndOwnerKeycloakId(projectId, userId)).thenReturn(true);

        assertThat(securityService.canViewProjectJoinRequests(
                projectId, authentication())).isTrue();
    }

    @Test
    void canCreateProjectJoinRequest_allowsRegularNonContributor() {
        assertThat(securityService.canCreateProjectJoinRequest(
                projectId, authentication())).isTrue();
    }

    @Test
    void canCreateProjectJoinRequest_deniesProjectMember() {
        when(projectRepository.existsByIdAndMembersKeycloakId(projectId, userId)).thenReturn(true);

        assertThat(securityService.canCreateProjectJoinRequest(
                projectId, authentication())).isFalse();
    }

    @Test
    void canManageProjectJoinRequests_checksProjectOwnerDirectly() {
        when(projectJoinRequestRepository.existsByIdAndProjectOwnerKeycloakId(resourceId, userId))
                .thenReturn(true);

        assertThat(securityService.canManageProjectJoinRequests(
                resourceId, authentication())).isTrue();
    }

    @Test
    void canCreateProjectPost_allowsContributor() {
        when(projectRepository.existsByIdAndMembersKeycloakId(projectId, userId)).thenReturn(true);

        assertThat(securityService.canCreateProjectPost(
                projectId, authentication())).isTrue();
    }

    @Test
    void canArchiveProjectPost_allowsPostAuthor() {
        when(projectPostRepository.existsByIdAndProjectIdAndAuthorKeycloakId(
                resourceId, projectId, userId)).thenReturn(true);

        assertThat(securityService.canArchiveProjectPost(
                projectId, resourceId, authentication())).isTrue();
    }

    @Test
    void canPublishDraft_allowsOnlyAuthor() {
        ProjectPostEntity post = ProjectPostEntity.builder()
                .id(resourceId)
                .status(ProjectPostStatus.DRAFT)
                .build();
        when(projectPostRepository.findByIdAndProjectId(resourceId, projectId))
                .thenReturn(Optional.of(post));
        when(projectPostRepository.existsByIdAndProjectIdAndAuthorKeycloakId(
                resourceId, projectId, userId)).thenReturn(true);

        assertThat(securityService.canPublishProjectPost(
                projectId, resourceId, authentication())).isTrue();
    }

    @Test
    void canPublishDraft_deniesProjectOwnerWhoIsNotAuthor() {
        ProjectPostEntity post = ProjectPostEntity.builder()
                .id(resourceId)
                .status(ProjectPostStatus.DRAFT)
                .build();
        when(projectPostRepository.findByIdAndProjectId(resourceId, projectId))
                .thenReturn(Optional.of(post));
        lenient().when(projectRepository.existsByIdAndOwnerKeycloakId(projectId, userId))
                .thenReturn(true);

        assertThat(securityService.canPublishProjectPost(
                projectId, resourceId, authentication())).isFalse();
    }

    @Test
    void canPublishArchivedPost_allowsProjectOwner() {
        ProjectPostEntity post = ProjectPostEntity.builder()
                .id(resourceId)
                .status(ProjectPostStatus.ARCHIVED)
                .build();
        when(projectPostRepository.findByIdAndProjectId(resourceId, projectId))
                .thenReturn(Optional.of(post));
        when(projectRepository.existsByIdAndOwnerKeycloakId(projectId, userId)).thenReturn(true);

        assertThat(securityService.canPublishProjectPost(
                projectId, resourceId, authentication())).isTrue();
    }

    @Test
    void canDeleteProjectPost_allowsOwner() {
        when(projectRepository.existsByIdAndOwnerKeycloakId(projectId, userId)).thenReturn(true);

        assertThat(securityService.canDeleteProjectPost(
                projectId, resourceId, authentication())).isTrue();
    }

    @Test
    void canViewAndEditOwnProfile() {
        when(userProfileRepository.existsByUsernameAndKeycloakId("Chris", userId))
                .thenReturn(true);

        assertThat(securityService.canViewUserProjects("Chris", authentication()))
                .isTrue();
        assertThat(securityService.canEditUserProfile("Chris", authentication()))
                .isTrue();
    }
    @Test
    void canDeleteUserProfile_allowsModerator() {
        assertThat(securityService.canDeleteUserProfile(
                "Chris", authentication("ROLE_MODERATOR"))).isTrue();
    }

    @Test
    void canFavoriteProject_allowsRegularUserWithProjectAccess() {
        when(projectRepository.existsByIdAndIsPrivateProjectFalse(projectId)).thenReturn(true);

        assertThat(securityService.canFavoriteProject(
                projectId, authentication())).isTrue();
    }

    @Test
    void canFavoriteProject_deniesModerator() {
        assertThat(securityService.canFavoriteProject(
                projectId, authentication("ROLE_MODERATOR"))).isFalse();
    }

    @Test
    void profileLinkPermissions_allowOwner() {
        Authentication auth = authentication();

        assertThat(securityService.canCreateUserProfileLinks(userId, auth)).isTrue();
        assertThat(securityService.canEditUserProfileLinks(userId, auth)).isTrue();
        assertThat(securityService.canDeleteUserProfileLinks(userId, auth)).isTrue();
    }

    @Test
    void canViewManagedUsers_shouldAllowModerator() {
        assertThat(securityService.canViewManagedUsers(authentication("ROLE_MODERATOR")))
                .isTrue();
    }

    @Test
    void canBanUser_shouldDenyModerator_whenBanningSelf() {
        assertThat(securityService.canBanUser(userId, authentication("ROLE_MODERATOR")))
                .isFalse();
    }

    @Test
    void canBanUser_shouldDenyRegularUser() {
        UUID otherUserId = UUID.randomUUID();

        assertThat(securityService.canBanUser(otherUserId, authentication()))
                .isFalse();
    }

    @Test
    void canBanUser_shouldAllowModerator_whenBanningOtherUser() {
        UUID otherUserId = UUID.randomUUID();

        when(userProfileRepository.existsByKeycloakIdAndStatus(otherUserId, UserStatus.ACTIVE))
                .thenReturn(true);
        assertThat(securityService.canBanUser(otherUserId, authentication("ROLE_MODERATOR")))
                .isTrue();
    }

    @Test
    void canCreateProject_shouldDenyBannedUser() {
        Authentication auth = authentication();

        when(userProfileRepository.existsByKeycloakIdAndStatus(userId, UserStatus.ACTIVE))
                .thenReturn(false);

        assertThat(securityService.canCreateProject(auth)).isFalse();
    }

    @Test
    void canUnbanUser_shouldAllowModerator_whenUserIsBanned() {
        UUID bannedUserId = UUID.randomUUID();

        when(userProfileRepository.existsByKeycloakIdAndStatus(bannedUserId, UserStatus.BANNED))
                .thenReturn(true);

        assertThat(securityService.canUnbanUser(bannedUserId, authentication("ROLE_MODERATOR")))
                .isTrue();
    }

    @Test
    void canUnbanUser_shouldDenyModerator_whenUserIsNotBanned() {
        UUID activeUserId = UUID.randomUUID();

        when(userProfileRepository.existsByKeycloakIdAndStatus(activeUserId, UserStatus.BANNED))
                .thenReturn(false);

        assertThat(securityService.canUnbanUser(activeUserId, authentication("ROLE_MODERATOR")))
                .isFalse();
    }

    @Test
    void canUnbanUser_shouldDenyRegularUser() {
        UUID bannedUserId = UUID.randomUUID();

        assertThat(securityService.canUnbanUser(bannedUserId, authentication()))
                .isFalse();
    }

    // --- thesis security ---

    @Test
    void canCreateThesis_allowsActiveProfessor() {
        when(userProfileRepository.existsByKeycloakIdAndIsProfessorTrue(userId)).thenReturn(true);

        assertThat(securityService.canCreateThesis(authentication())).isTrue();
    }

    @Test
    void canCreateThesis_deniesRegularUser() {
        when(userProfileRepository.existsByKeycloakIdAndIsProfessorTrue(userId)).thenReturn(false);

        assertThat(securityService.canCreateThesis(authentication())).isFalse();
    }

    @Test
    void canCreateThesis_deniesModerator() {
        assertThat(securityService.canCreateThesis(authentication("ROLE_MODERATOR"))).isFalse();
    }

    @Test
    void canCreateThesis_deniesInactiveProfessor() {
        when(userProfileRepository.existsByKeycloakIdAndStatus(userId, UserStatus.ACTIVE))
                .thenReturn(false);

        assertThat(securityService.canCreateThesis(authentication())).isFalse();
    }

    @Test
    void canEditThesis_allowsSupervisingProfessor() {
        UUID thesisId = UUID.randomUUID();
        when(userProfileRepository.existsByKeycloakIdAndIsProfessorTrue(userId)).thenReturn(true);
        when(thesisRepository.existsByIdAndSupervisorKeycloakId(thesisId, userId)).thenReturn(true);

        assertThat(securityService.canEditThesis(thesisId, authentication())).isTrue();
    }

    @Test
    void canEditThesis_deniesNonSupervisor() {
        UUID thesisId = UUID.randomUUID();
        when(userProfileRepository.existsByKeycloakIdAndIsProfessorTrue(userId)).thenReturn(true);
        when(thesisRepository.existsByIdAndSupervisorKeycloakId(thesisId, userId)).thenReturn(false);

        assertThat(securityService.canEditThesis(thesisId, authentication())).isFalse();
    }

    @Test
    void canEditThesis_deniesInactiveProfessor() {
        UUID thesisId = UUID.randomUUID();
        when(userProfileRepository.existsByKeycloakIdAndStatus(userId, UserStatus.ACTIVE))
                .thenReturn(false);

        assertThat(securityService.canEditThesis(thesisId, authentication())).isFalse();
    }

    @Test
    void canDeleteThesis_allowsModerator() {
        UUID thesisId = UUID.randomUUID();

        assertThat(securityService.canDeleteThesis(thesisId, authentication("ROLE_MODERATOR"))).isTrue();
    }

    @Test
    void canDeleteThesis_allowsActiveSupervisingProfessor() {
        UUID thesisId = UUID.randomUUID();
        when(userProfileRepository.existsByKeycloakIdAndIsProfessorTrue(userId)).thenReturn(true);
        when(thesisRepository.existsByIdAndSupervisorKeycloakId(thesisId, userId)).thenReturn(true);

        assertThat(securityService.canDeleteThesis(thesisId, authentication())).isTrue();
    }

    @Test
    void canDeleteThesis_deniesInactiveSupervisingProfessor() {
        UUID thesisId = UUID.randomUUID();
        when(userProfileRepository.existsByKeycloakIdAndStatus(userId, UserStatus.ACTIVE))
                .thenReturn(false);

        assertThat(securityService.canDeleteThesis(thesisId, authentication())).isFalse();
    }

    @Test
    void canDeleteThesis_deniesNonSupervisorProfessor() {
        UUID thesisId = UUID.randomUUID();
        when(userProfileRepository.existsByKeycloakIdAndIsProfessorTrue(userId)).thenReturn(true);
        when(thesisRepository.existsByIdAndSupervisorKeycloakId(thesisId, userId)).thenReturn(false);

        assertThat(securityService.canDeleteThesis(thesisId, authentication())).isFalse();
    }

    @Test
    void canManageThesisStudents_allowsActiveSupervisingProfessor() {
        UUID thesisId = UUID.randomUUID();
        when(userProfileRepository.existsByKeycloakIdAndIsProfessorTrue(userId)).thenReturn(true);
        when(thesisRepository.existsByIdAndSupervisorKeycloakId(thesisId, userId)).thenReturn(true);

        assertThat(securityService.canManageThesisStudents(thesisId, authentication())).isTrue();
    }

    @Test
    void canManageThesisStudents_deniesRegularUser() {
        UUID thesisId = UUID.randomUUID();
        when(userProfileRepository.existsByKeycloakIdAndIsProfessorTrue(userId)).thenReturn(false);

        assertThat(securityService.canManageThesisStudents(thesisId, authentication())).isFalse();
    }
}