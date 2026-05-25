package de.thm.swtp.api.search.repository;

import de.thm.swtp.api.userprofile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserSearchRepository extends JpaRepository<UserProfile, UUID> {

    List<UserProfile> findByUsernameContainingIgnoreCase(String username);
}
