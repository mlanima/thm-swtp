package de.thm.swtp.api.tag;

import de.thm.swtp.api.tag.domain.Tag;
import de.thm.swtp.api.tag.entity.TagEntity;
import de.thm.swtp.api.tag.repository.TagRepository;
import de.thm.swtp.api.tag.service.ProfileTagService;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
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

class ProfileTagServiceTest {

    private TagRepository tagRepository;
    private UserProfileRepository userProfileRepository;
    private ProfileTagService profileTagService;

    private UUID userId;
    private UserProfile userProfile;

    @BeforeEach
    void setup() {
        tagRepository = mock(TagRepository.class);
        userProfileRepository = mock(UserProfileRepository.class);

        profileTagService = new ProfileTagService(tagRepository, userProfileRepository);

        userId = UUID.randomUUID();

        userProfile = new UserProfile();
        userProfile.setKeycloakId(userId);
        userProfile.setTags(new HashSet<>());
    }

    @Test
    void getUserProfileTags_shouldReturnProfileTags() {
        userProfile.getTags().add(new TagEntity("Java"));
        userProfile.getTags().add(new TagEntity("Angular"));

        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile));

        List<Tag> result = profileTagService.getUserProfileTags(userId);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Tag::getName)
                .containsExactlyInAnyOrder("Java", "Angular");
    }

    @Test
    void getUserProfileTags_shouldThrow_whenUserProfileDoesNotExist() {
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileTagService.getUserProfileTags(userId))
                .isInstanceOf(UserProfileNotFoundException.class)
                .hasMessage("Profile not found for user: " + userId);
    }

    @Test
    void addTagToProfile_shouldCreateAndAssignTag_whenTagDoesNotExist() {
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(tagRepository.findByNameIgnoreCase("Spring")).thenReturn(Optional.empty());
        when(tagRepository.save(any(TagEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tag result = profileTagService.addTagToProfile(userId, "Spring");

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

        Tag result = profileTagService.addTagToProfile(userId, "Java");

        assertThat(result.getName()).isEqualTo("Java");
        assertThat(userProfile.getTags()).contains(existingTag);

        verify(tagRepository, never()).save(any());
    }

    @Test
    void addTagToProfile_shouldTrimTagName_whenCreatingTag() {
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(tagRepository.findByNameIgnoreCase("Spring")).thenReturn(Optional.empty());
        when(tagRepository.save(any(TagEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tag result = profileTagService.addTagToProfile(userId, "  Spring  ");

        assertThat(result.getName()).isEqualTo("Spring");
        assertThat(userProfile.getTags())
                .extracting(TagEntity::getName)
                .containsExactly("Spring");
    }

    @Test
    void addTagToProfile_shouldThrow_whenUserProfileDoesNotExist() {
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileTagService.addTagToProfile(userId, "Spring"))
                .isInstanceOf(UserProfileNotFoundException.class)
                .hasMessage("Profile not found for user: " + userId);

        verify(tagRepository, never()).save(any());
    }
}
