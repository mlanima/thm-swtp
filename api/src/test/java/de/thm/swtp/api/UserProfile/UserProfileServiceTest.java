package de.thm.swtp.api.UserProfile;
import de.thm.swtp.api.location.GooglePlacesClient;
import de.thm.swtp.api.location.exception.InvalidPlaceException;
import de.thm.swtp.api.moderation.ContentModerationService;
import de.thm.swtp.api.moderation.exception.ContentNotValidException;
import de.thm.swtp.api.userprofile.domain.UserStatus;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import de.thm.swtp.api.userprofile.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private ContentModerationService contentModerationService;

    @Mock
    private GooglePlacesClient googlePlacesClient;

    private UserProfileService userProfileService;

    private UUID userId;
    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService(userProfileRepository, contentModerationService, googlePlacesClient);

        userId = UUID.randomUUID();

        userProfile = UserProfile.builder()
                .keycloakId(userId)
                .username("Chris")
                .email("chris@example.com")
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void getUsersByStatus_shouldReturnPagedUsers() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<UserProfile> page = new PageImpl<>(List.of(userProfile), pageable, 1);

        when(userProfileRepository.findByStatus(UserStatus.ACTIVE, pageable))
                .thenReturn(page);

        Page<UserProfile> result = userProfileService.getUsersByStatus(UserStatus.ACTIVE, pageable);

        assertThat(result.getContent()).containsExactly(userProfile);
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(userProfileRepository).findByStatus(UserStatus.ACTIVE, pageable);
    }

    @Test
    void banUser_shouldSetStatusToBanned() {
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(userProfileRepository.save(userProfile)).thenReturn(userProfile);

        UserProfile result = userProfileService.banUser(userId, "Spam");

        assertThat(result.getStatus()).isEqualTo(UserStatus.BANNED);
        assertThat(result.getBanReason()).isEqualTo("Spam");
        assertThat(result.getBannedAt()).isNotNull();
        assertThat(userProfile.getStatus()).isEqualTo(UserStatus.BANNED);

        verify(userProfileRepository).save(userProfile);
    }

    @Test
    void unbanUser_shouldSetStatusToActive() {
        userProfile.setStatus(UserStatus.BANNED);
        userProfile.setBanReason("Spam");
        userProfile.setBannedAt(java.time.LocalDateTime.now());

        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(userProfileRepository.save(userProfile)).thenReturn(userProfile);

        UserProfile result = userProfileService.unbanUser(userId);

        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.getBanReason()).isNull();
        assertThat(result.getBannedAt()).isNull();

        assertThat(userProfile.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(userProfile.getBanReason()).isNull();
        assertThat(userProfile.getBannedAt()).isNull();

        verify(userProfileRepository).save(userProfile);
    }

    @Test
    void banUser_shouldThrow_whenUserDoesNotExist() {
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.banUser(userId, "Spam"))
                .isInstanceOf(UserProfileNotFoundException.class);

        verify(userProfileRepository).findById(userId);
    }

    // ── updateProfile — moderation ───────────────────────────────────────────────

    @Test
    void updateProfile_shouldModerateTitleAndSave() {
        when(userProfileRepository.findByUsername("Chris")).thenReturn(Optional.of(userProfile));
        when(userProfileRepository.save(userProfile)).thenReturn(userProfile);

        userProfileService.updateProfile("Chris", "Clean Title", null, null, null, null);

        verify(contentModerationService).assertAppropriate("Clean Title", "title");
        verify(contentModerationService, never()).assertAppropriate(any(), eq("about"));
        verify(contentModerationService, never()).assertAppropriate(any(), eq("experience"));
        verify(userProfileRepository).save(userProfile);
    }

    @Test
    void updateProfile_shouldModerateAllTextFields() {
        when(userProfileRepository.findByUsername("Chris")).thenReturn(Optional.of(userProfile));
        when(userProfileRepository.save(userProfile)).thenReturn(userProfile);

        userProfileService.updateProfile("Chris", "Title", null, "About", "Exp", null);

        verify(contentModerationService).assertAppropriate("Title", "title");
        verify(contentModerationService).assertAppropriate("About", "about");
        verify(contentModerationService).assertAppropriate("Exp", "experience");
        verify(userProfileRepository).save(userProfile);
    }

    @Test
    void updateProfile_shouldSkipNullFields() {
        when(userProfileRepository.findByUsername("Chris")).thenReturn(Optional.of(userProfile));
        when(userProfileRepository.save(userProfile)).thenReturn(userProfile);

        userProfileService.updateProfile("Chris", null, null, null, null, null);

        verify(contentModerationService, never()).assertAppropriate(any(), any());
        verify(userProfileRepository).save(userProfile);
    }

    @Test
    void updateProfile_shouldThrow_whenTitleFlagged() {
        when(userProfileRepository.findByUsername("Chris")).thenReturn(Optional.of(userProfile));
        var ex = new ContentNotValidException("title");
        org.mockito.Mockito.doThrow(ex).when(contentModerationService).assertAppropriate("bad", "title");

        assertThatThrownBy(() -> userProfileService.updateProfile("Chris", "bad", null, null, null, null))
                .isInstanceOf(ContentNotValidException.class);

        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void updateProfile_shouldThrow_whenAboutFlagged() {
        when(userProfileRepository.findByUsername("Chris")).thenReturn(Optional.of(userProfile));
        var ex = new ContentNotValidException("about");
        org.mockito.Mockito.doThrow(ex).when(contentModerationService).assertAppropriate("bad", "about");

        assertThatThrownBy(() -> userProfileService.updateProfile("Chris", "title", null, "bad", null, null))
                .isInstanceOf(ContentNotValidException.class);

        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void updateProfile_shouldThrow_whenExperienceFlagged() {
        when(userProfileRepository.findByUsername("Chris")).thenReturn(Optional.of(userProfile));
        var ex = new ContentNotValidException("experience");
        org.mockito.Mockito.doThrow(ex).when(contentModerationService).assertAppropriate("bad", "experience");

        assertThatThrownBy(() -> userProfileService.updateProfile("Chris", "title", null, null, "bad", null))
                .isInstanceOf(ContentNotValidException.class);

        verify(userProfileRepository, never()).save(any());
    }

    // ── updateProfile — location / placeId ───────────────────────────────────────

    @Test
    void updateProfile_shouldSetLocation_whenPlaceIdValid() {
        when(userProfileRepository.findByUsername("Chris")).thenReturn(Optional.of(userProfile));
        when(googlePlacesClient.validatePlaceId("ChIJ...")).thenReturn("Munich, Germany");
        when(userProfileRepository.save(userProfile)).thenReturn(userProfile);

        userProfileService.updateProfile("Chris", null, "Munich", null, null, "ChIJ...");

        assertThat(userProfile.getLocation()).isEqualTo("Munich, Germany");
        assertThat(userProfile.getPlaceId()).isEqualTo("ChIJ...");
        verify(userProfileRepository).save(userProfile);
    }

    @Test
    void updateProfile_shouldClearLocation_whenLocationBlank() {
        userProfile.setLocation("Old City");
        userProfile.setPlaceId("OldPlaceId");
        when(userProfileRepository.findByUsername("Chris")).thenReturn(Optional.of(userProfile));
        when(userProfileRepository.save(userProfile)).thenReturn(userProfile);

        userProfileService.updateProfile("Chris", null, "", null, null, "anything");

        assertThat(userProfile.getLocation()).isNull();
        assertThat(userProfile.getPlaceId()).isNull();
        verify(googlePlacesClient, never()).validatePlaceId(any());
    }

    @Test
    void updateProfile_shouldSkipLocation_whenPlaceIdNull() {
        when(userProfileRepository.findByUsername("Chris")).thenReturn(Optional.of(userProfile));
        when(userProfileRepository.save(userProfile)).thenReturn(userProfile);

        userProfileService.updateProfile("Chris", null, "Munich", null, null, null);

        verify(googlePlacesClient, never()).validatePlaceId(any());
    }

    @Test
    void updateProfile_shouldThrow_whenLocationWithoutPlaceId() {
        when(userProfileRepository.findByUsername("Chris")).thenReturn(Optional.of(userProfile));

        assertThatThrownBy(() -> userProfileService.updateProfile("Chris", null, "Munich", null, null, ""))
                .isInstanceOf(InvalidPlaceException.class);

        verify(userProfileRepository, never()).save(any());
    }
}