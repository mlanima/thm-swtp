package de.thm.swtp.api.userFollow.repository;

import de.thm.swtp.api.userFollow.entity.UserFollowEntity;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserFollowRepository extends JpaRepository<UserFollowEntity, UUID> {

    boolean existsByFollowerKeycloakIdAndFollowingKeycloakId(UUID followerId, UUID followingId);

    Optional<UserFollowEntity> findByFollowerKeycloakIdAndFollowingKeycloakId(UUID followerId, UUID followingId);

    long countByFollowingKeycloakId(UUID followingId);

    @Query("SELECT f.follower FROM UserFollowEntity f WHERE f.following.keycloakId = :followingId")
    List<UserProfile> findFollowersByFollowingId(@Param("followingId") UUID followingId);

    @Query("SELECT f.following FROM UserFollowEntity f WHERE f.follower.keycloakId = :followerId")
    List<UserProfile> findFollowingByFollowerId(@Param("followerId") UUID followerId);
}
