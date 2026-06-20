package de.thm.swtp.api.userFollow.service;

import de.thm.swtp.api.userFollow.entity.UserFollowEntity;
import de.thm.swtp.api.userFollow.exception.CannotFollowYourselfException;
import de.thm.swtp.api.userFollow.exception.UserAlreadyFollowingException;
import de.thm.swtp.api.userFollow.exception.UserFollowNotFoundException;
import de.thm.swtp.api.userFollow.repository.UserFollowRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserFollowService {

    private final UserFollowRepository userFollowRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public void follow(UUID followerId, UUID followingId) {
        if (followerId.equals(followingId)) {
            throw new CannotFollowYourselfException();
        }

        if (userFollowRepository.existsByFollowerKeycloakIdAndFollowingKeycloakId(followerId, followingId)) {
            throw new UserAlreadyFollowingException();
        }

        UserProfile follower = userProfileRepository.findById(followerId)
                .orElseThrow(() -> new UserProfileNotFoundException(followerId.toString()));

        UserProfile following = userProfileRepository.findById(followingId)
                .orElseThrow(() -> new UserProfileNotFoundException(followingId.toString()));

        userFollowRepository.save(
                UserFollowEntity.builder()
                        .follower(follower)
                        .following(following)
                        .build()
        );

        following.setFollowers(following.getFollowers() + 1);
        userProfileRepository.save(following);
    }

    @Transactional
    public void unfollow(UUID followerId, UUID followingId) {
        UserFollowEntity follow = userFollowRepository
                .findByFollowerKeycloakIdAndFollowingKeycloakId(followerId, followingId)
                .orElseThrow(UserFollowNotFoundException::new);

        userFollowRepository.delete(follow);

        UserProfile following = follow.getFollowing();
        following.setFollowers(Math.max(0, following.getFollowers() - 1));
        userProfileRepository.save(following);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(UUID followerId, UUID followingId) {
        return userFollowRepository.existsByFollowerKeycloakIdAndFollowingKeycloakId(followerId, followingId);
    }

    @Transactional(readOnly = true)
    public long countFollowers(UUID followingId) {
        return userFollowRepository.countByFollowingKeycloakId(followingId);
    }

    @Transactional(readOnly = true)
    public List<UserProfile> getFollowers(UUID followingId) {
        return userFollowRepository.findFollowersByFollowingId(followingId);
    }

    @Transactional(readOnly = true)
    public List<UserProfile> getFollowing(UUID followerId) {
        return userFollowRepository.findFollowingByFollowerId(followerId);
    }
}
