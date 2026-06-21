package de.thm.swtp.api.userFollow;

import de.thm.swtp.api.userFollow.entity.UserFollowEntity;
import de.thm.swtp.api.userFollow.exception.CannotFollowYourselfException;
import de.thm.swtp.api.userFollow.exception.UserAlreadyFollowingException;
import de.thm.swtp.api.userFollow.exception.UserFollowNotFoundException;
import de.thm.swtp.api.userFollow.repository.UserFollowRepository;
import de.thm.swtp.api.userFollow.service.UserFollowService;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserFollowServiceTest {

    @Mock
    private UserFollowRepository userFollowRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserFollowService userFollowService;

    @Test
    void shouldFollowUser() {
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();

        UserProfile follower = UserProfile.builder()
                .keycloakId(followerId)
                .username("follower")
                .email("follower@mni.thm.de")
                .build();

        UserProfile following = UserProfile.builder()
                .keycloakId(followingId)
                .username("following")
                .email("following@mni.thm.de")
                .build();

        when(userFollowRepository.existsByFollowerKeycloakIdAndFollowingKeycloakId(followerId, followingId))
                .thenReturn(false);
        when(userProfileRepository.getReferenceById(followerId)).thenReturn(follower);
        when(userProfileRepository.getReferenceById(followingId)).thenReturn(following);

        userFollowService.follow(followerId, followingId);

        verify(userFollowRepository).save(any(UserFollowEntity.class));
        verify(userProfileRepository).incrementFollowers(followingId);
    }

    @Test
    void shouldThrowExceptionWhenFollowingYourself() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> userFollowService.follow(userId, userId))
                .isInstanceOf(CannotFollowYourselfException.class);

        verify(userFollowRepository, never()).save(any(UserFollowEntity.class));
    }

    @Test
    void shouldThrowExceptionWhenAlreadyFollowing() {
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();

        when(userFollowRepository.existsByFollowerKeycloakIdAndFollowingKeycloakId(followerId, followingId))
                .thenReturn(true);

        assertThatThrownBy(() -> userFollowService.follow(followerId, followingId))
                .isInstanceOf(UserAlreadyFollowingException.class);

        verify(userFollowRepository, never()).save(any(UserFollowEntity.class));
    }

    @Test
    void shouldUnfollowUser() {
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();

        UserFollowEntity follow = UserFollowEntity.builder()
                .id(UUID.randomUUID())
                .build();

        when(userFollowRepository.findByFollowerKeycloakIdAndFollowingKeycloakId(followerId, followingId))
                .thenReturn(Optional.of(follow));

        userFollowService.unfollow(followerId, followingId);

        verify(userFollowRepository).delete(follow);
        verify(userProfileRepository).decrementFollowers(followingId);
    }

    @Test
    void shouldThrowExceptionWhenFollowNotFound() {
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();

        when(userFollowRepository.findByFollowerKeycloakIdAndFollowingKeycloakId(followerId, followingId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userFollowService.unfollow(followerId, followingId))
                .isInstanceOf(UserFollowNotFoundException.class);

        verify(userFollowRepository, never()).delete(any(UserFollowEntity.class));
    }

    @Test
    void shouldReturnTrueWhenFollowing() {
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();

        when(userFollowRepository.existsByFollowerKeycloakIdAndFollowingKeycloakId(followerId, followingId))
                .thenReturn(true);

        assertThat(userFollowService.isFollowing(followerId, followingId)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenNotFollowing() {
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();

        when(userFollowRepository.existsByFollowerKeycloakIdAndFollowingKeycloakId(followerId, followingId))
                .thenReturn(false);

        assertThat(userFollowService.isFollowing(followerId, followingId)).isFalse();
    }

    @Test
    void shouldReturnFollowerCount() {
        UUID followingId = UUID.randomUUID();

        when(userFollowRepository.countByFollowingKeycloakId(followingId)).thenReturn(5L);

        assertThat(userFollowService.countFollowers(followingId)).isEqualTo(5L);
    }
}
