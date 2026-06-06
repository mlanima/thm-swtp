package de.thm.swtp.api.search.repository;

import de.thm.swtp.api.userprofile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    /**
     * Fetches user profiles by their IDs with their tags eagerly loaded.
     * <p>
     * Uses {@code JOIN FETCH} to load the {@code tags} collection within the
     * same query, avoiding a {@link org.hibernate.LazyInitializationException}
     * when the tags are accessed outside the Hibernate session (e.g. in a mapper).
     *
     * @param ids user profile IDs to fetch
     * @return user profiles with their tags initialized
     */
    @Query("""
            SELECT DISTINCT u FROM user_profiles u
            LEFT JOIN FETCH u.tags
            WHERE u.keycloakId IN :ids
            """)
    List<UserProfile> findAllWithTagsById(@Param("ids") Collection<UUID> ids);
}
