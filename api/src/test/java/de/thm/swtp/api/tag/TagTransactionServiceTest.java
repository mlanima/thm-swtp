package de.thm.swtp.api.tag;

import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import de.thm.swtp.api.tag.domain.Tag;
import de.thm.swtp.api.tag.entity.TagEntity;
import de.thm.swtp.api.tag.repository.TagRepository;
import de.thm.swtp.api.tag.service.TagTransactionService;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TagTransactionServiceTest {

    private TagRepository tagRepository;
    private UserProfileRepository userProfileRepository;
    private ProjectRepository projectRepository;
    private TagTransactionService tagTransactionService;

    private UUID userId;
    private UUID projectId;
    private UserProfile userProfile;
    private ProjectEntity project;

    @BeforeEach
    void setup() {
        tagRepository = mock(TagRepository.class);
        userProfileRepository = mock(UserProfileRepository.class);
        projectRepository = mock(ProjectRepository.class);

        tagTransactionService = new TagTransactionService(tagRepository, userProfileRepository, projectRepository);

        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        userProfile = new UserProfile();
        userProfile.setKeycloakId(userId);
        userProfile.setTags(new HashSet<>());

        project = new ProjectEntity();
        project.setId(projectId);
        project.setTags(new HashSet<>());
    }

    @Test
    void addTagToProfile_shouldCreateAndSaveTag_whenTagDoesNotExist() {
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(tagRepository.findByNameIgnoreCase("Spring")).thenReturn(Optional.empty());
        when(tagRepository.save(any(TagEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tag result = tagTransactionService.addTagToProfile(userId, "Spring");

        assertThat(result.getName()).isEqualTo("Spring");
        assertThat(userProfile.getTags())
                .extracting(TagEntity::getName)
                .containsExactly("Spring");

        ArgumentCaptor<TagEntity> captor = ArgumentCaptor.forClass(TagEntity.class);
        verify(tagRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Spring");
    }

    @Test
    void addTagToProfile_shouldUseExistingTag_whenTagAlreadyExists() {
        TagEntity existingTag = new TagEntity("Java");
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(tagRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(existingTag));

        Tag result = tagTransactionService.addTagToProfile(userId, "Java");

        assertThat(result.getName()).isEqualTo("Java");
        assertThat(userProfile.getTags()).contains(existingTag);
        verify(tagRepository, never()).save(any());
    }

    @Test
    void addTagToProfile_shouldThrow_whenUserProfileDoesNotExist() {
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagTransactionService.addTagToProfile(userId, "Spring"))
                .isInstanceOf(UserProfileNotFoundException.class)
                .hasMessage("Profile not found for user: " + userId);

        verify(tagRepository, never()).findByNameIgnoreCase(any());
        verify(tagRepository, never()).save(any());
    }

    @Test
    void addTagToProject_shouldCreateAndSaveTag_whenTagDoesNotExist() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(tagRepository.findByNameIgnoreCase("Spring")).thenReturn(Optional.empty());
        when(tagRepository.save(any(TagEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tag result = tagTransactionService.addTagToProject(projectId, "Spring");

        assertThat(result.getName()).isEqualTo("Spring");
        assertThat(project.getTags())
                .extracting(TagEntity::getName)
                .containsExactly("Spring");

        ArgumentCaptor<TagEntity> captor = ArgumentCaptor.forClass(TagEntity.class);
        verify(tagRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Spring");
    }

    @Test
    void addTagToProject_shouldUseExistingTag_whenTagAlreadyExists() {
        TagEntity existingTag = new TagEntity("Java");
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(tagRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(existingTag));

        Tag result = tagTransactionService.addTagToProject(projectId, "Java");

        assertThat(result.getName()).isEqualTo("Java");
        assertThat(project.getTags()).contains(existingTag);
        verify(tagRepository, never()).save(any());
    }

    @Test
    void addTagToProject_shouldThrow_whenProjectDoesNotExist() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagTransactionService.addTagToProject(projectId, "Spring"))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessage("Project not found: " + projectId);

        verify(tagRepository, never()).findByNameIgnoreCase(any());
        verify(tagRepository, never()).save(any());
    }
}
