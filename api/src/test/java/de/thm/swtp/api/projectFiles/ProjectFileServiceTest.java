package de.thm.swtp.api.projectFiles;

import de.thm.swtp.api.exceptionhandling.exceptions.ProjectFileDoesNotBelongToProjectException;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectFileNotFoundException;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import de.thm.swtp.api.projectFiles.domain.FileVisibility;
import de.thm.swtp.api.projectFiles.domain.ProjectFile;
import de.thm.swtp.api.projectFiles.entity.ProjectFileEntity;
import de.thm.swtp.api.projectFiles.repository.ProjectFileRepository;
import de.thm.swtp.api.projectFiles.service.ProjectFileService;
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

public class ProjectFileServiceTest {

    private ProjectFileRepository projectFileRepository;
    private ProjectRepository projectRepository;
    private ProjectFileService projectFileService;

    private UUID projectId;
    private UUID otherProjectId;
    private UUID ownerId;
    private UUID otherUserId;
    private UUID fileId;
    private UUID memberId;

    private ProjectEntity project;
    private ProjectEntity otherProject;
    private UserProfile owner;
    private UserProfile member;

    @BeforeEach
    void setUp() {
        projectFileRepository = mock(ProjectFileRepository.class);
        projectRepository = mock(ProjectRepository.class);

        projectFileService = new ProjectFileService(
                projectFileRepository,
                projectRepository
        );

        projectId = UUID.randomUUID();
        otherProjectId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        fileId = UUID.randomUUID();
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
    void getProjectFiles_shouldReturnAllFiles_whenCurrentUserIsProjectOwner() {
        ProjectFileEntity publicFile = createProjectFileEntity(project, "public.pdf", FileVisibility.PUBLIC);
        ProjectFileEntity privateFile = createProjectFileEntity(project, "private.pdf", FileVisibility.PRIVATE);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectFileRepository.findByProjectIdOrderByCreatedAtAsc(projectId))
                .thenReturn(List.of(publicFile, privateFile));

        List<ProjectFile> result = projectFileService.getProjectFiles(projectId, ownerId);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(ProjectFile::getVisibility)
                .containsExactly(FileVisibility.PUBLIC, FileVisibility.PRIVATE);

        verify(projectFileRepository).findByProjectIdOrderByCreatedAtAsc(projectId);
        verify(projectFileRepository, never()).findByProjectIdAndVisibilityOrderByCreatedAtAsc(any(), any());
    }

    @Test
    void getProjectFiles_shouldReturnAllFiles_whenCurrentUserIsProjectMember() {
        ProjectFileEntity privateFile = createProjectFileEntity(project, "private.pdf", FileVisibility.PRIVATE);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectFileRepository.findByProjectIdOrderByCreatedAtAsc(projectId))
                .thenReturn(List.of(privateFile));

        List<ProjectFile> result = projectFileService.getProjectFiles(projectId, memberId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getVisibility()).isEqualTo(FileVisibility.PRIVATE);

        verify(projectFileRepository).findByProjectIdOrderByCreatedAtAsc(projectId);
        verify(projectFileRepository, never()).findByProjectIdAndVisibilityOrderByCreatedAtAsc(any(), any());
    }

    @Test
    void getProjectFiles_shouldReturnOnlyPublicFiles_whenCurrentUserIsNotProjectContributor() {
        ProjectFileEntity publicFile = createProjectFileEntity(project, "public.pdf", FileVisibility.PUBLIC);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectFileRepository.findByProjectIdAndVisibilityOrderByCreatedAtAsc(
                projectId,
                FileVisibility.PUBLIC
        )).thenReturn(List.of(publicFile));

        List<ProjectFile> result = projectFileService.getProjectFiles(projectId, otherUserId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getVisibility()).isEqualTo(FileVisibility.PUBLIC);

        verify(projectFileRepository).findByProjectIdAndVisibilityOrderByCreatedAtAsc(projectId, FileVisibility.PUBLIC);
        verify(projectFileRepository, never()).findByProjectIdOrderByCreatedAtAsc(projectId);
    }

    @Test
    void getProjectFiles_shouldThrowProjectNotFound_whenProjectDoesNotExist() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectFileService.getProjectFiles(projectId, ownerId))
                .isInstanceOf(ProjectNotFoundException.class);

        verify(projectFileRepository, never()).findByProjectIdOrderByCreatedAtAsc(any());
        verify(projectFileRepository, never()).findByProjectIdAndVisibilityOrderByCreatedAtAsc(any(), any());
    }

    @Test
    void updateFileVisibility_shouldUpdateVisibility_whenFileBelongsToProject() {
        ProjectFileEntity file = createProjectFileEntity(project, "doc.pdf", FileVisibility.PUBLIC);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectFileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(projectFileRepository.save(file)).thenReturn(file);

        ProjectFile result = projectFileService.updateFileVisibility(projectId, fileId, FileVisibility.PRIVATE);

        assertThat(result.getVisibility()).isEqualTo(FileVisibility.PRIVATE);
        verify(projectFileRepository).save(file);
    }

    @Test
    void updateFileVisibility_shouldThrowFileNotFound_whenFileDoesNotExist() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectFileRepository.findById(fileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectFileService.updateFileVisibility(projectId, fileId, FileVisibility.PRIVATE))
                .isInstanceOf(ProjectFileNotFoundException.class);

        verify(projectFileRepository, never()).save(any());
    }

    @Test
    void updateFileVisibility_shouldThrow_whenFileDoesNotBelongToProject() {
        ProjectFileEntity file = createProjectFileEntity(otherProject, "doc.pdf", FileVisibility.PUBLIC);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectFileRepository.findById(fileId)).thenReturn(Optional.of(file));

        assertThatThrownBy(() -> projectFileService.updateFileVisibility(projectId, fileId, FileVisibility.PRIVATE))
                .isInstanceOf(ProjectFileDoesNotBelongToProjectException.class);

        verify(projectFileRepository, never()).save(any());
    }

    @Test
    void updateFileVisibility_shouldThrowProjectNotFound_whenProjectDoesNotExist() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectFileService.updateFileVisibility(projectId, fileId, FileVisibility.PRIVATE))
                .isInstanceOf(ProjectNotFoundException.class);

        verify(projectFileRepository, never()).findById(any());
        verify(projectFileRepository, never()).save(any());
    }

    private ProjectFileEntity createProjectFileEntity(
            ProjectEntity owningProject,
            String originalName,
            FileVisibility visibility) {
        return ProjectFileEntity.builder()
                .id(fileId)
                .project(owningProject)
                .originalName(originalName)
                .storageName(UUID.randomUUID() + ".pdf")
                .mimeType("application/pdf")
                .sizeBytes(1024)
                .createdAt(LocalDateTime.now())
                .visibility(visibility)
                .build();
    }
}