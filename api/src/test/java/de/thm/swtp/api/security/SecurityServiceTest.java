package de.thm.swtp.api.security;

import de.thm.swtp.api.config.SecurityService;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.projectInvitation.repository.ProjectInviteRepository;
import de.thm.swtp.api.projectJoinRequest.repository.ProjectJoinRequestRepository;
import de.thm.swtp.api.projectPost.domain.ProjectPostStatus;
import de.thm.swtp.api.projectPost.entity.ProjectPostEntity;
import de.thm.swtp.api.projectPost.repository.ProjectPostRepository;
import de.thm.swtp.api.userAccount.domain.UserRole;
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

import java.util.Arrays;
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

    private Authentication authentication(UserRole... roles) {
        List<GrantedAuthority> authorities = Arrays.stream(roles)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .map(GrantedAuthority.class::cast)
                .toList();

        return new UsernamePasswordAuthenticationToken(
                userId.toString(), null, authorities);
    }

    @Test
    void canViewProject_returnsTrue_forPublicProject() {
        when(projectRepository.existsByIdAndIsPrivateProjectFalse(projectId)).thenReturn(true);

        assertThat(securityService.canViewProject(projectId, authentication(UserRole.USER)))
                .isTrue();
    }

    @Test
    void canViewProject_returnsTrue_forProjectMember() {
        when(projectRepository.existsByIdAndMembersKeycloakId(projectId, userId)).thenReturn(true);

        assertThat(securityService.canViewProject(projectId, authentication(UserRole.USER)))
                .isTrue();
    }

    @Test
    void canViewProject_returnsTrue_forAdmin() {
        assertThat(securityService.canViewProject(projectId, authentication(UserRole.ADMIN)))
                .isTrue();
    }

    @Test
    void canViewProject_returnsFalse_forPrivateProjectAndStranger() {
        assertThat(securityService.canViewProject(projectId, authentication(UserRole.USER)))
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

        assertThat(securityService.canViewProjectByUrl("demo", authentication(UserRole.USER)))
                .isTrue();
    }

    @Test
    void canCreateProject_allowsRegularUser_butNotAdmin() {
        assertThat(securityService.canCreateProject(authentication(UserRole.USER))).isTrue();
        assertThat(securityService.canCreateProject(authentication(UserRole.ADMIN))).isFalse();
        assertThat(securityService.canCreateProject(authentication(UserRole.USER, UserRole.ADMIN))).isFalse();
    }

    @Test
    void canEditProject_allowsOwner() {
        when(projectRepository.existsByIdAndOwnerKeycloakId(projectId, userId)).thenReturn(true);

        assertThat(securityService.canEditProject(projectId, authentication(UserRole.USER)))
                .isTrue();
    }

    @Test
    void canEditProject_deniesNonOwner() {
        assertThat(securityService.canEditProject(projectId, authentication(UserRole.USER)))
                .isFalse();
    }

    @Test
    void canDeleteProject_allowsAdmin() {
        assertThat(securityService.canDeleteProject(projectId, authentication(UserRole.ADMIN)))
                .isTrue();
    }

    @Test
    void canViewProjectMembers_usesProjectVisibility() {
        when(projectRepository.existsByIdAndIsPrivateProjectFalse(projectId)).thenReturn(true);

        assertThat(securityService.canViewProjectMembers(projectId, authentication(UserRole.USER)))
                .isTrue();
    }

    @Test
    void canRemoveProjectMember_allowsOwnerRemovingAnotherUser() {
        UUID memberId = UUID.randomUUID();
        when(projectRepository.existsByIdAndOwnerKeycloakId(projectId, userId)).thenReturn(true);

        assertThat(securityService.canRemoveProjectMember(
                projectId, memberId, authentication(UserRole.USER))).isTrue();
    }

    @Test
    void canRemoveProjectMember_deniesRemovingSelf() {
        when(projectRepository.existsByIdAndOwnerKeycloakId(projectId, userId)).thenReturn(true);

        assertThat(securityService.canRemoveProjectMember(
                projectId, userId, authentication(UserRole.USER))).isFalse();
    }

    @Test
    void canCreateProjectInvitation_allowsProjectOwner() {
        when(projectRepository.existsByIdAndOwnerKeycloakId(projectId, userId)).thenReturn(true);

        assertThat(securityService.canCreateProjectInvitation(
                projectId, authentication(UserRole.USER))).isTrue();
    }


    @Test
    void canRespondToProjectInvite_checksInvitedUser() {
        when(projectInviteRepository.existsByIdAndInvitedUserKeycloakId(resourceId, userId))
                .thenReturn(true);

        assertThat(securityService.canRespondToProjectInvite(
                resourceId, authentication(UserRole.USER))).isTrue();
    }

    @Test
    void canViewProjectJoinRequests_allowsOwner() {
        when(projectRepository.existsByIdAndOwnerKeycloakId(projectId, userId)).thenReturn(true);

        assertThat(securityService.canViewProjectJoinRequests(
                projectId, authentication(UserRole.USER))).isTrue();
    }

    @Test
    void canCreateProjectJoinRequest_allowsRegularNonContributor() {
        assertThat(securityService.canCreateProjectJoinRequest(
                projectId, authentication(UserRole.USER))).isTrue();
    }

    @Test
    void canCreateProjectJoinRequest_deniesProjectMember() {
        when(projectRepository.existsByIdAndMembersKeycloakId(projectId, userId)).thenReturn(true);

        assertThat(securityService.canCreateProjectJoinRequest(
                projectId, authentication(UserRole.USER))).isFalse();
    }

    @Test
    void canManageProjectJoinRequests_checksProjectOwnerDirectly() {
        when(projectJoinRequestRepository.existsByIdAndProjectOwnerKeycloakId(resourceId, userId))
                .thenReturn(true);

        assertThat(securityService.canManageProjectJoinRequests(
                resourceId, authentication(UserRole.USER))).isTrue();
    }

    @Test
    void canCreateProjectPost_allowsContributor() {
        when(projectRepository.existsByIdAndMembersKeycloakId(projectId, userId)).thenReturn(true);

        assertThat(securityService.canCreateProjectPost(
                projectId, authentication(UserRole.USER))).isTrue();
    }

    @Test
    void canArchiveProjectPost_allowsPostAuthor() {
        when(projectPostRepository.existsByIdAndProjectIdAndAuthorKeycloakId(
                resourceId, projectId, userId)).thenReturn(true);

        assertThat(securityService.canArchiveProjectPost(
                projectId, resourceId, authentication(UserRole.USER))).isTrue();
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
                projectId, resourceId, authentication(UserRole.USER))).isTrue();
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
                projectId, resourceId, authentication(UserRole.USER))).isFalse();
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
                projectId, resourceId, authentication(UserRole.USER))).isTrue();
    }

    @Test
    void canDeleteProjectPost_allowsOwner() {
        when(projectRepository.existsByIdAndOwnerKeycloakId(projectId, userId)).thenReturn(true);

        assertThat(securityService.canDeleteProjectPost(
                projectId, resourceId, authentication(UserRole.USER))).isTrue();
    }

    @Test
    void canViewAndEditOwnProfile() {
        when(userProfileRepository.existsByUsernameAndKeycloakId("Chris", userId))
                .thenReturn(true);

        assertThat(securityService.canViewUserProjects("Chris", authentication(UserRole.USER)))
                .isTrue();
        assertThat(securityService.canEditUserProfile("Chris", authentication(UserRole.USER)))
                .isTrue();
    }
    @Test
    void canDeleteUserProfile_allowsAdmin() {
        assertThat(securityService.canDeleteUserProfile(
                "Chris", authentication(UserRole.ADMIN))).isTrue();
    }

    @Test
    void canFavoriteProject_allowsRegularUserWithProjectAccess() {
        when(projectRepository.existsByIdAndIsPrivateProjectFalse(projectId)).thenReturn(true);

        assertThat(securityService.canFavoriteProject(
                projectId, authentication(UserRole.USER))).isTrue();
    }

    @Test
    void canFavoriteProject_deniesAdmin() {
        assertThat(securityService.canFavoriteProject(
                projectId, authentication(UserRole.ADMIN))).isFalse();
    }

    @Test
    void profileLinkPermissions_allowOwner() {
        Authentication auth = authentication(UserRole.USER);

        assertThat(securityService.canCreateUserProfileLinks(userId, auth)).isTrue();
        assertThat(securityService.canEditUserProfileLinks(userId, auth)).isTrue();
        assertThat(securityService.canDeleteUserProfileLinks(userId, auth)).isTrue();
    }
}