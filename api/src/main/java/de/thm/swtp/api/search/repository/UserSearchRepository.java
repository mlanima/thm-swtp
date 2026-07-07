package de.thm.swtp.api.search.repository;

import de.thm.swtp.api.userprofile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository for searching {@link UserProfile} instances by username or tags.
 * <p>
 * Uses a single JPQL query with a LEFT JOIN to the tags association.
 * Banned users ({@code status = BANNED}) are always excluded.
 */
public interface UserSearchRepository extends JpaRepository<UserProfile, UUID> {

    /**
     * Returns only the IDs of user profiles matching the given query term and filters.
     * Matches are performed case-insensitively against the username
     * and assigned tag names. Banned users are always excluded.
     * <p>
     * Every filter parameter is optional: passing {@code null} (or, for
     * {@code tags}/{@code tagCount}, an empty list/zero) leaves that
     * constraint unapplied.
     * <p>
     * Returning just the IDs avoids loading full entities during the
     * intersection phase, which is more efficient for multi-term search.
     *
     * @param query         a single search term
     * @param isProfessor   restrict to verified professors (true) or non-professors (false); {@code null} = no constraint
     * @param tags          tag names to match against (any one must be present); ignored when {@code tagCount} is 0
     * @param tagCount      number of entries in {@code tags}; must be provided separately since JPQL cannot check collection size of a bind parameter
     * @param location      substring to match against the user's location, case-insensitive; {@code null} = no constraint
     * @param createdAfter  restrict to users created at or after this timestamp; {@code null} = no constraint
     * @param createdBefore restrict to users created at or before this timestamp; {@code null} = no constraint
     * @return distinct user profile IDs matching the term and all given filters
     */
    @Query("""
            SELECT DISTINCT u.keycloakId FROM user_profiles u
            LEFT JOIN u.tags t
            WHERE u.status = de.thm.swtp.api.userprofile.domain.UserStatus.ACTIVE
            AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')))
            AND (:isProfessor IS NULL OR u.isProfessor = :isProfessor)
            AND (:location IS NULL OR LOWER(u.location) LIKE LOWER(CONCAT('%', :location, '%')))
            AND (:createdAfter IS NULL OR u.createdAt >= :createdAfter)
            AND (:createdBefore IS NULL OR u.createdAt <= :createdBefore)
            AND (:tagCount = 0 OR EXISTS (SELECT 1 FROM u.tags ft WHERE ft.name IN :tags))
            """)
    List<UUID> searchIdsByQuery(
            @Param("query") String query,
            @Param("isProfessor") Boolean isProfessor,
            @Param("tags") Collection<String> tags,
            @Param("tagCount") int tagCount,
            @Param("location") String location,
            @Param("createdAfter") LocalDateTime createdAfter,
            @Param("createdBefore") LocalDateTime createdBefore
    );

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
