package de.thm.swtp.api.tag;

import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import de.thm.swtp.api.tag.domain.Tag;
import de.thm.swtp.api.tag.entity.TagEntity;
import de.thm.swtp.api.tag.repository.TagRepository;
import de.thm.swtp.api.tag.service.ProjectTagService;
import de.thm.swtp.api.tag.validation.TagValidationService;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectTagServiceTest {

    private TagRepository tagRepository;
    private ProjectRepository projectRepository;
    private TagValidationService tagValidationService;
    private ProjectTagService projectTagService;

    private UUID projectId;
    private UUID ownerId;

    private ProjectEntity project;
    private UserProfile owner;

    @BeforeEach
    void setup() {
        tagRepository = mock(TagRepository.class);
        projectRepository = mock(ProjectRepository.class);
        tagValidationService = mock(TagValidationService.class);
        when(tagValidationService.isValidTag(any())).thenReturn(true);

        projectTagService = new ProjectTagService(tagRepository, projectRepository, tagValidationService);

        projectId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        owner = new UserProfile();
        owner.setKeycloakId(ownerId);

        project = new ProjectEntity();
        project.setId(projectId);
        project.setOwner(owner);
        project.setTags(new HashSet<>());
    }

    @Test
    void getProjectTags_shouldReturnProjectTags() {
        project.getTags().add(new TagEntity("Java"));
        project.getTags().add(new TagEntity("Angular"));

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        List<Tag> result = projectTagService.getProjectTags(projectId);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Tag::getName)
                .containsExactlyInAnyOrder("Java", "Angular");
    }

    @Test
    void getProjectTags_shouldThrow_whenProjectDoesNotExist() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectTagService.getProjectTags(projectId))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessage("Project not found: " + projectId);
    }

    @Test
    void addTagToProject_shouldCreateAndAssignTag_whenTagDoesNotExistAndUserIsOwner() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(tagRepository.findByNameIgnoreCase("Spring")).thenReturn(Optional.empty());
        when(tagRepository.save(any(TagEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tag result = projectTagService.addTagToProject(projectId, "Spring");

        assertThat(result.getName()).isEqualTo("Spring");
        assertThat(project.getTags())
                .extracting(TagEntity::getName)
                .containsExactly("Spring");

        ArgumentCaptor<TagEntity> captor = ArgumentCaptor.forClass(TagEntity.class);
        verify(tagRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Spring");
    }

    @Test
    void addTagToProject_shouldUseExistingTag_whenTagAlreadyExistsAndUserIsOwner() {
        TagEntity existingTag = new TagEntity("Java");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(tagRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(existingTag));

        Tag result = projectTagService.addTagToProject(projectId, "Java");

        assertThat(result.getName()).isEqualTo("Java");
        assertThat(project.getTags()).contains(existingTag);

        verify(tagRepository, never()).save(any());
    }

    @Test
    void addTagToProject_shouldTrimTagName_whenCreatingTag() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(tagRepository.findByNameIgnoreCase("Spring")).thenReturn(Optional.empty());
        when(tagRepository.save(any(TagEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tag result = projectTagService.addTagToProject(projectId, "  Spring  ");

        assertThat(result.getName()).isEqualTo("Spring");
        assertThat(project.getTags())
                .extracting(TagEntity::getName)
                .containsExactly("Spring");
    }

    @Test
    void addTagToProject_shouldThrow_whenProjectDoesNotExist() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectTagService.addTagToProject(projectId, "Spring"))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessage("Project not found: " + projectId);

        verify(tagRepository, never()).save(any());
    }

    @Test
    void removeTagFromProject_shouldRemoveTag_whenTagExistsAndUserIsOwner() {
        TagEntity tag = new TagEntity("Angular");
        project.getTags().add(tag);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(tagRepository.findByNameIgnoreCase("Angular")).thenReturn(Optional.of(tag));

        projectTagService.removeTagFromProject(projectId, "Angular");

        assertThat(project.getTags()).doesNotContain(tag);
        verify(tagRepository, never()).delete(any());
    }

    @Test
    void removeTagFromProject_shouldTrimTagName_whenRemovingTag() {
        TagEntity tag = new TagEntity("Angular");
        project.getTags().add(tag);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(tagRepository.findByNameIgnoreCase("Angular")).thenReturn(Optional.of(tag));

        projectTagService.removeTagFromProject(projectId, "  Angular  ");

        assertThat(project.getTags()).doesNotContain(tag);
        verify(tagRepository).findByNameIgnoreCase("Angular");
        verify(tagRepository, never()).delete(any());
    }

    @Test
    void removeTagFromProject_shouldDoNothing_whenTagDoesNotExistAndUserIsOwner() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(tagRepository.findByNameIgnoreCase("Angular")).thenReturn(Optional.empty());

        projectTagService.removeTagFromProject(projectId, "Angular");

        assertThat(project.getTags()).isEmpty();
        verify(tagRepository, never()).delete(any());
    }

    @Test
    void removeTagFromProject_shouldThrow_whenProjectDoesNotExist() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectTagService.removeTagFromProject(projectId, "Angular"))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessage("Project not found: " + projectId);

        verify(tagRepository, never()).findByNameIgnoreCase(any());
        verify(tagRepository, never()).delete(any());
    }
}