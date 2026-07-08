package de.thm.swtp.api.userprofile.repository;

import de.thm.swtp.api.userprofile.domain.UserStatus;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUsername(String username);
    boolean existsByUsernameAndKeycloakId(String username, UUID keycloakId);
    boolean existsByKeycloakIdAndIsProfessorTrue(UUID keycloakId);

    @Modifying
    @Query("UPDATE user_profiles u SET u.followers = u.followers + 1 WHERE u.keycloakId = :keycloakId")
    void incrementFollowers(@Param("keycloakId") UUID keycloakId);

    @Modifying
    @Query("UPDATE user_profiles u SET u.followers = CASE WHEN u.followers > 0 THEN u.followers - 1 ELSE 0 END WHERE u.keycloakId = :keycloakId")
    void decrementFollowers(@Param("keycloakId") UUID keycloakId);


    Page<UserProfile> findByStatus(UserStatus status, Pageable pageable);
    boolean existsByKeycloakIdAndStatus(UUID keycloakId, UserStatus status);

    Optional<UserProfile> findByKeycloakId(UUID keycloakId);
}
