package de.thm.swtp.api.search.repository;

import de.thm.swtp.api.userprofile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for searching {@link UserProfile} instances by username or tags.
 * <p>
 * Uses a single JPQL query with a LEFT JOIN to the tags association.
 */
public interface UserSearchRepository extends JpaRepository<UserProfile, UUID> {

    /**
     * Returns only the IDs of user profiles matching the given query term.
     * Matches are performed case-insensitively against the username
     * and assigned tag names.
     * <p>
     * Returning just the IDs avoids loading full entities during the
     * intersection phase, which is more efficient for multi-term search.
     *
     * @param query a single search term
     * @return distinct user profile IDs matching the term
     */
    @Query("""
            SELECT DISTINCT u.keycloakId FROM user_profiles u
            LEFT JOIN u.tags t
            WHERE (LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    List<UUID> searchIdsByQuery(@Param("query") String query);
}
