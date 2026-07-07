package de.thm.swtp.api.search.repository;

import de.thm.swtp.api.project.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository for searching {@link ProjectEntity} instances by name or tags.
 * <p>
 * Uses a single JPQL query with a LEFT JOIN to the tags association.
 * Soft-deleted projects ({@code deletedAt IS NOT NULL}) are excluded.
 */
public interface ProjectSearchRepository extends JpaRepository<ProjectEntity, UUID> {

    /**
     * Returns only the IDs of projects matching the given query term and filters.
     * Matches are performed case-insensitively against the project name
     * and assigned tag names. Soft-deleted projects are excluded.
     * <p>
     * Every filter parameter is optional: passing {@code null} (or, for
     * {@code tags}/{@code tagCount}, an empty list/zero) leaves that
     * constraint unapplied.
     * <p>
     * Returning just the IDs avoids loading full entities during the
     * intersection phase, which is more efficient for multi-term search.
     *
     * @param query              a single search term
     * @param hasOpenPositions   restrict to projects with (true) or without (false) open positions; {@code null} = no constraint
     * @param allowJoinRequests  restrict to projects accepting (or not) join requests; {@code null} = no constraint
     * @param tags               tag names to match against (any one must be present); ignored when {@code tagCount} is 0
     * @param tagCount           number of entries in {@code tags}; must be provided separately since JPQL cannot check collection size of a bind parameter
     * @param createdAfter       restrict to projects created at or after this timestamp; {@code null} = no constraint
     * @param createdBefore      restrict to projects created at or before this timestamp; {@code null} = no constraint
     * @return distinct project IDs matching the term and all given filters
     */
    @Query("""
            SELECT DISTINCT p.id FROM projects p
            LEFT JOIN p.tags t
            WHERE p.deletedAt IS NULL
            AND p.isPrivateProject = false
            AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')))
            AND (:hasOpenPositions IS NULL
                 OR (:hasOpenPositions = TRUE AND p.openPositionsCount > 0)
                 OR (:hasOpenPositions = FALSE AND p.openPositionsCount = 0))
            AND (:allowJoinRequests IS NULL OR p.allowJoinRequests = :allowJoinRequests)
            AND (:createdAfter IS NULL OR p.createdAt >= :createdAfter)
            AND (:createdBefore IS NULL OR p.createdAt <= :createdBefore)
            AND (:tagCount = 0 OR EXISTS (SELECT 1 FROM p.tags ft WHERE ft.name IN :tags))
            """)
    List<UUID> searchIdsByQuery(
            @Param("query") String query,
            @Param("hasOpenPositions") Boolean hasOpenPositions,
            @Param("allowJoinRequests") Boolean allowJoinRequests,
            @Param("tags") Collection<String> tags,
            @Param("tagCount") int tagCount,
            @Param("createdAfter") LocalDateTime createdAfter,
            @Param("createdBefore") LocalDateTime createdBefore
    );

    /**
     * Fetches projects by their IDs with their tags eagerly loaded.
     * <p>
     * Uses {@code JOIN FETCH} to load the {@code tags} collection within the
     * same query, avoiding a {@link org.hibernate.LazyInitializationException}
     * when the tags are accessed outside the Hibernate session (e.g. in a mapper).
     *
     * @param ids project IDs to fetch
     * @return projects with their tags initialized
     */
    @Query("""
            SELECT DISTINCT p FROM projects p
            LEFT JOIN FETCH p.tags
            WHERE p.id IN :ids
            """)
    List<ProjectEntity> findAllWithTagsById(@Param("ids") Collection<UUID> ids);
}
