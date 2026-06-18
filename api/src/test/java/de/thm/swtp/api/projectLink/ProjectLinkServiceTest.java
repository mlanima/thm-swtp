package de.thm.swtp.api.projectLink;

import de.thm.swtp.api.exceptionhandling.exceptions.ProjectLinkAlreadyExistsException;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectLinkNotFoundException;
import de.thm.swtp.api.links.domain.LinkVisibility;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ExceptionProjectEditNotAllowed;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import de.thm.swtp.api.links.domain.ProjectLink;
import de.thm.swtp.api.links.entity.ProjectLinkEntity;
import de.thm.swtp.api.links.repository.ProjectLinkRepository;
import de.thm.swtp.api.links.service.ProjectLinkService;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ProjectLinkServiceTest {

    private ProjectLinkRepository projectLinkRepository;
    private ProjectRepository projectRepository;
    private ProjectLinkService projectLinkService;

    private UUID projectId;
    private UUID otherProjectId;
    private UUID ownerId;
    private UUID otherUserId;
    private UUID linkId;
    private UUID memberId;

    private ProjectEntity project;
    private ProjectEntity otherProject;
    private UserProfile owner;
    private UserProfile member;

    String url= "https://github.com/mlanima/thm-swtp";
    String label = "Github";

    @BeforeEach
    void setUp() {
        projectLinkRepository = mock(ProjectLinkRepository.class);
        projectRepository = mock(ProjectRepository.class);

        projectLinkService = new ProjectLinkService(
                projectLinkRepository,
                projectRepository
        );

        projectId = UUID.randomUUID();
        otherProjectId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        linkId = UUID.randomUUID();
        memberId = UUID.randomUUID();

        owner = new UserProfile();
        owner.setKeycloakId(ownerId);

        member = new UserProfile();
        member.setKeycloakId(memberId);

        project = new ProjectEntity();
        project.setId(projectId);
        project.setOwner(owner);
        project.setMembers(new HashSet<>());
        project.getMembers().add(member);

        otherProject = new ProjectEntity();
        otherProject.setId(otherProjectId);
        otherProject.setOwner(owner);
    }

    @Test
    void createProjectLink_shouldCreateLink_whenCurrentUserIsOwnerAndUrlDoesNotExist() {

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectLinkRepository.existsByProjectIdAndUrlIgnoreCase(
                projectId,
                url
        )).thenReturn(false);

        when(projectLinkRepository.save(any(ProjectLinkEntity.class)))
                .thenAnswer(invocation -> {
                    ProjectLinkEntity entity = invocation.getArgument(0);
                    entity.setId(linkId);
                    entity.setCreatedAt(LocalDateTime.now());
                    entity.setUpdatedAt(LocalDateTime.now());
                    return entity;
                });

        ProjectLink result = projectLinkService.createProjectLink(
                projectId,
                ownerId,
                label,
                url,
                LinkVisibility.PUBLIC
        );

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(linkId);
        assertThat(result.getProjectId()).isEqualTo(projectId);
        assertThat(result.getLabel()).isEqualTo(label);
        assertThat(result.getUrl()).isEqualTo(url);
        assertThat(result.getVisibility()).isEqualTo(LinkVisibility.PUBLIC);

        verify(projectLinkRepository).save(any(ProjectLinkEntity.class));
    }

    @Test
    void createProjectLink_shouldThrowProjectNotFound_whenProjectDoesNotExist() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectLinkService.createProjectLink(
                projectId,
                ownerId,
                label,
                url,
                LinkVisibility.PUBLIC
        )).isInstanceOf(ProjectNotFoundException.class);

        verify(projectLinkRepository, never()).save(any());
    }

    @Test
    void createProjectLink_shouldThrowAccessDenied_whenCurrentUserIsNotOwner() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectLinkService.createProjectLink(
                projectId,
                otherUserId,
                label,
                url,
                LinkVisibility.PUBLIC
        )).isInstanceOf(ExceptionProjectEditNotAllowed.class);

        verify(projectLinkRepository, never()).save(any());
    }

    @Test
    void createProjectLink_shouldThrowAlreadyExists_whenUrlAlreadyExistsForProject() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectLinkRepository.existsByProjectIdAndUrlIgnoreCase(
                projectId,
                url
        )).thenReturn(true);

        assertThatThrownBy(() -> projectLinkService.createProjectLink(
                projectId,
                ownerId,
                label,
                url,
                LinkVisibility.PUBLIC
        )).isInstanceOf(ProjectLinkAlreadyExistsException.class);

        verify(projectLinkRepository, never()).save(any());
    }

    @Test
    void getProjectLinks_shouldReturnAllLinks_whenCurrentUserIsProjectOwner() {
        ProjectLinkEntity publicLink = ProjectLinkEntity.builder()
                .id(UUID.randomUUID())
                .project(project)
                .label(label)
                .url(url)
                .visibility(LinkVisibility.PUBLIC)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ProjectLinkEntity privateLink = ProjectLinkEntity.builder()
                .id(UUID.randomUUID())
                .project(project)
                .label("Private Link")
                .url("https://private-url-github.com")
                .visibility(LinkVisibility.PRIVATE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectLinkRepository.findByProjectIdOrderByCreatedAtAsc(projectId))
                .thenReturn(List.of(publicLink, privateLink));

        List<ProjectLink> result = projectLinkService.getProjectLinks(projectId, ownerId);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(ProjectLink::getVisibility)
                .containsExactly(LinkVisibility.PUBLIC, LinkVisibility.PRIVATE);

        verify(projectLinkRepository).findByProjectIdOrderByCreatedAtAsc(projectId);
        verify(projectLinkRepository, never()).findByProjectIdAndVisibilityOrderByCreatedAtAsc(any(), any());
    }

    @Test
    void getProjectLinks_shouldReturnAllLinks_whenCurrentUserIsProjectMember() {
        ProjectLinkEntity privateLink = ProjectLinkEntity.builder()
                .id(linkId)
                .project(project)
                .label(label)
                .url(url)
                .visibility(LinkVisibility.PRIVATE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectLinkRepository.findByProjectIdOrderByCreatedAtAsc(projectId))
                .thenReturn(List.of(privateLink));

        List<ProjectLink> result = projectLinkService.getProjectLinks(projectId, memberId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getVisibility()).isEqualTo(LinkVisibility.PRIVATE);

        verify(projectLinkRepository).findByProjectIdOrderByCreatedAtAsc(projectId);
        verify(projectLinkRepository, never()).findByProjectIdAndVisibilityOrderByCreatedAtAsc(any(), any());
    }

    @Test
    void getProjectLinks_shouldReturnOnlyPublicLinks_whenCurrentUserIsNotProjectContributor() {
        ProjectLinkEntity publicLink = ProjectLinkEntity.builder()
                .id(linkId)
                .project(project)
                .label(label)
                .url(url)
                .visibility(LinkVisibility.PUBLIC)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectLinkRepository.findByProjectIdAndVisibilityOrderByCreatedAtAsc(
                projectId,
                LinkVisibility.PUBLIC
        )).thenReturn(List.of(publicLink));

        List<ProjectLink> result = projectLinkService.getProjectLinks(projectId, otherUserId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getVisibility()).isEqualTo(LinkVisibility.PUBLIC);

        verify(projectLinkRepository).findByProjectIdAndVisibilityOrderByCreatedAtAsc(projectId, LinkVisibility.PUBLIC);
        verify(projectLinkRepository, never()).findByProjectIdOrderByCreatedAtAsc(projectId);
    }

    @Test
    void getProjectLinks_shouldThrowProjectNotFound_whenProjectDoesNotExist() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectLinkService.getProjectLinks(projectId, ownerId))
                .isInstanceOf(ProjectNotFoundException.class);

        verify(projectLinkRepository, never()).findByProjectIdOrderByCreatedAtAsc(any());
        verify(projectLinkRepository, never()).findByProjectIdAndVisibilityOrderByCreatedAtAsc(any(), any());
    }

    @Test
    void createProjectLink_shouldDefaultToPublic_whenVisibilityIsNull() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectLinkRepository.existsByProjectIdAndUrlIgnoreCase(projectId, url)).thenReturn(false);

        when(projectLinkRepository.save(any(ProjectLinkEntity.class)))
                .thenAnswer(invocation -> {
                    ProjectLinkEntity entity = invocation.getArgument(0);
                    entity.setId(linkId);
                    entity.setCreatedAt(LocalDateTime.now());
                    entity.setUpdatedAt(LocalDateTime.now());
                    return entity;
                });

        ProjectLink result = projectLinkService.createProjectLink(
                projectId,
                ownerId,
                label,
                url,
                null
        );

        assertThat(result.getVisibility()).isEqualTo(LinkVisibility.PUBLIC);
    }

    @Test
    void updateProjectLink_shouldUpdateOnlyLabel_whenUrlIsNull() {
        ProjectLinkEntity link = ProjectLinkEntity.builder()
                .id(linkId)
                .project(project)
                .label(label)
                .url(url)
                .visibility(LinkVisibility.PUBLIC)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectLinkRepository.findById(linkId)).thenReturn(Optional.of(link));
        when(projectLinkRepository.save(link)).thenReturn(link);

        ProjectLink result = projectLinkService.updateProjectLink(
                projectId,
                ownerId,
                linkId,
                "New Label",
                null,
                null
        );

        assertThat(result.getLabel()).isEqualTo("New Label");
        assertThat(result.getUrl()).isEqualTo(url);

        verify(projectLinkRepository, never()).existsByProjectIdAndUrlIgnoreCase(any(), any());
        verify(projectLinkRepository).save(link);
    }

    @Test
    void updateProjectLink_shouldUpdateOnlyUrl_whenLabelIsNullAndUrlChanged() {
        ProjectLinkEntity link = ProjectLinkEntity.builder()
                .id(linkId)
                .project(project)
                .label(label)
                .url(url)
                .visibility(LinkVisibility.PUBLIC)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectLinkRepository.findById(linkId)).thenReturn(Optional.of(link));
        when(projectLinkRepository.existsByProjectIdAndUrlIgnoreCase(
                projectId,
                "https://github.com/new-project"
        )).thenReturn(false);
        when(projectLinkRepository.save(link)).thenReturn(link);

        ProjectLink result = projectLinkService.updateProjectLink(
                projectId,
                ownerId,
                linkId,
                null,
                "https://github.com/new-project",
                null
        );

        assertThat(result.getLabel()).isEqualTo(label);
        assertThat(result.getUrl()).isEqualTo("https://github.com/new-project");

        verify(projectLinkRepository).existsByProjectIdAndUrlIgnoreCase(
                projectId,
                "https://github.com/new-project"
        );
        verify(projectLinkRepository).save(link);
    }

    @Test
    void updateProjectLink_shouldUpdateLabelAndUrl_whenBothAreProvided() {
        ProjectLinkEntity link = ProjectLinkEntity.builder()
                .id(linkId)
                .project(project)
                .label(label)
                .url(label)
                .visibility(LinkVisibility.PUBLIC)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectLinkRepository.findById(linkId)).thenReturn(Optional.of(link));
        when(projectLinkRepository.existsByProjectIdAndUrlIgnoreCase(
                projectId,
                "https://github.com/new-project"
        )).thenReturn(false);
        when(projectLinkRepository.save(link)).thenReturn(link);

        ProjectLink result = projectLinkService.updateProjectLink(
                projectId,
                ownerId,
                linkId,
                "New Label",
                "https://github.com/new-project",
                null
        );

        assertThat(result.getLabel()).isEqualTo("New Label");
        assertThat(result.getUrl()).isEqualTo("https://github.com/new-project");

        verify(projectLinkRepository).save(link);
    }

    @Test
    void updateProjectLink_shouldThrowAccessDenied_whenCurrentUserIsNotOwner() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectLinkService.updateProjectLink(
                projectId,
                otherUserId,
                linkId,
                "New Label",
                null,
                null
        )).isInstanceOf(ExceptionProjectEditNotAllowed.class);

        verify(projectLinkRepository, never()).findById(any());
        verify(projectLinkRepository, never()).save(any());
    }

    @Test
    void updateProjectLink_shouldThrowLinkNotFound_whenLinkDoesNotExist() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectLinkRepository.findById(linkId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectLinkService.updateProjectLink(
                projectId,
                ownerId,
                linkId,
                "New Label",
                null,
                null
        )).isInstanceOf(ProjectLinkNotFoundException.class);

        verify(projectLinkRepository, never()).save(any());
    }

    @Test
    void updateProjectLink_shouldUpdateVisibility_whenVisibilityIsProvided() {
        ProjectLinkEntity link = ProjectLinkEntity.builder()
                .id(linkId)
                .project(project)
                .label(label)
                .url(url)
                .visibility(LinkVisibility.PUBLIC)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectLinkRepository.findById(linkId)).thenReturn(Optional.of(link));
        when(projectLinkRepository.save(link)).thenReturn(link);

        ProjectLink result = projectLinkService.updateProjectLink(
                projectId,
                ownerId,
                linkId,
                null,
                null,
                LinkVisibility.PRIVATE
        );

        assertThat(result.getVisibility()).isEqualTo(LinkVisibility.PRIVATE);
        verify(projectLinkRepository).save(link);
    }



    @Test
    void deleteProjectLink_shouldDeleteLink_whenCurrentUserIsOwnerAndLinkBelongsToProject() {
        ProjectLinkEntity link = ProjectLinkEntity.builder()
                .id(linkId)
                .project(project)
                .label(label)
                .url(url)
                .visibility(LinkVisibility.PUBLIC)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectLinkRepository.findById(linkId)).thenReturn(Optional.of(link));

        projectLinkService.deleteProjectLink(projectId, ownerId, linkId);

        verify(projectLinkRepository).delete(link);
    }

    @Test
    void deleteProjectLink_shouldThrowAccessDenied_whenCurrentUserIsNotOwner() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectLinkService.deleteProjectLink(
                projectId,
                otherUserId,
                linkId
        )).isInstanceOf(ExceptionProjectEditNotAllowed.class);

        verify(projectLinkRepository, never()).findById(any());
        verify(projectLinkRepository, never()).delete(any());
    }
}
