package de.thm.swtp.api.links.repository;

import de.thm.swtp.api.links.entity.UserProfileLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.UUID;

public interface UserProfileLinkRepository extends JpaRepository<UserProfileLinkEntity, UUID> {

    List<UserProfileLinkEntity> findByUserProfileKeycloakIdOrderByCreatedAtAsc(UUID keycloakId);
    boolean existsByUserProfileKeycloakIdAndUrlIgnoreCase(UUID keycloakId, String url);
}
