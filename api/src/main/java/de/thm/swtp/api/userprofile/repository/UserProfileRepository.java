package de.thm.swtp.api.userprofile.repository;

import de.thm.swtp.api.userprofile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUsername(String username);

}
