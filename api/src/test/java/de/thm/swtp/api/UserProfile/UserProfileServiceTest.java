package de.thm.swtp.api.UserProfile;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    private UserProfileService userProfileService;

    private UUID userId;
    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService(userProfileRepository);

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
}