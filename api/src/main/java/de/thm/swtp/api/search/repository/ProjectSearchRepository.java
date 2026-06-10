package de.thm.swtp.api.search.repository;

import de.thm.swtp.api.project.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * Returns only the IDs of projects matching the given query term.
     * Matches are performed case-insensitively against the project name
     * and assigned tag names. Soft-deleted projects are excluded.
     * <p>
     * Returning just the IDs avoids loading full entities during the
     * intersection phase, which is more efficient for multi-term search.
     *
     * @param query a single search term
     * @return distinct project IDs matching the term
     */
    @Query("""
            SELECT DISTINCT p.id FROM projects p
            LEFT JOIN p.tags t
            WHERE p.deletedAt IS NULL
            AND p.isPrivateProject = false
            AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    List<UUID> searchIdsByQuery(@Param("query") String query);

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
