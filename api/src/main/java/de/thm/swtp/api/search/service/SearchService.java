package de.thm.swtp.api.search.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generic search engine that intersects multiple query terms using ID-based set operations,
 * with optional pagination.
 * <p>
 * Each query term is executed as its own independent database query (with a separate JOIN
 * to the tags association), avoiding the cross-term tag matching problem where a single
 * tag would need to satisfy all terms simultaneously. The resulting entity IDs are then
 * intersected to enforce AND logic across all terms.
 * <p>
 * Pagination is applied <em>after</em> all IDs are intersected, ensuring correct page
 * boundaries even when multiple query terms are involved.
 * <p>
 * This class is designed to be reused by any entity type that needs search-by-name-or-tags
 * functionality. The caller supplies the ID-search and batch-fetch functions as method
 * references.
 *
 * <h3>Usage examples</h3>
 * <pre>{@code
 * // Without pagination
 * searchService.search(queries, repository::searchIdsByQuery, repository::findAllById);
 *
 * // With pagination
 * searchService.search(queries, repository::searchIdsByQuery, repository::findAllById, pageable);
 * }</pre>
 */
@Component
@RequiredArgsConstructor
public class SearchService {

    /**
     * Executes a multi-term search with AND semantics.
     *
     * @param <T>             the entity type being searched
     * @param queries         search terms; each term is matched case-insensitively against
     *                        the entity's name/username field and its assigned tags
     * @param searchIdsByQuery function that runs a single-term query and returns matching
     *                         entity IDs (lightweight, avoids loading full entities)
     * @param findAllById     function that batch-fetches entities by their IDs
     * @return list of entities matching all query terms, or empty list if none match
     */
    public <T> List<T> search(
            List<String> queries,
            Function<String, List<UUID>> searchIdsByQuery,
            Function<Collection<UUID>, List<T>> findAllById
    ) {
        Set<UUID> ids = intersectIds(queries, searchIdsByQuery);
        if (ids.isEmpty()) {
            return List.of();
        }
        return findAllById.apply(ids);
    }

    /**
     * Executes a paginated multi-term search with AND semantics.
     *
     * @param <T>             the entity type being searched
     * @param queries         search terms; each term is matched case-insensitively against
     *                        the entity's name/username field and its assigned tags
     * @param searchIdsByQuery function that runs a single-term query and returns matching
     *                         entity IDs (lightweight, avoids loading full entities)
     * @param findAllById     function that batch-fetches entities by their IDs
     * @param pageable        pagination and sorting information
     * @return a {@link Page} of entities matching all query terms, never {@code null}
     */
    public <T> Page<T> search(
            List<String> queries,
            Function<String, List<UUID>> searchIdsByQuery,
            Function<Collection<UUID>, List<T>> findAllById,
            Pageable pageable
    ) {
        return search(queries, searchIdsByQuery, findAllById, pageable, null, null);
    }

    /**
     * Executes a paginated multi-term search with AND semantics, ordering results
     * by a custom ranking score rather than by raw ID.
     *
     * @param <T>             the entity type being searched
     * @param queries         search terms; each term is matched case-insensitively against
     *                        the entity's name/username field and its assigned tags
     * @param searchIdsByQuery function that runs a single-term query and returns matching
     *                         entity IDs (lightweight, avoids loading full entities)
     * @param findAllById     function that batch-fetches entities by their IDs
     * @param pageable        pagination and sorting information
     * @param sortKeyLookup   optional function returning a ranking score per ID (higher first);
     *                        IDs missing from the result default to 0. If {@code null}, results
     *                        are ordered by raw ID.
     * @param idExtractor     optional function extracting the ID from a fetched entity, used to
     *                        restore the ranked order after {@code findAllById} (whose result
     *                        order is not guaranteed). Required if {@code sortKeyLookup} is given.
     * @return a {@link Page} of entities matching all query terms, never {@code null}
     */
    public <T> Page<T> search(
            List<String> queries,
            Function<String, List<UUID>> searchIdsByQuery,
            Function<Collection<UUID>, List<T>> findAllById,
            Pageable pageable,
            Function<Collection<UUID>, Map<UUID, Long>> sortKeyLookup,
            Function<T, UUID> idExtractor
    ) {
        Set<UUID> ids = intersectIds(queries, searchIdsByQuery);
        if (ids.isEmpty()) {
            return Page.empty(pageable);
        }

        List<UUID> sortedIds = new ArrayList<>(ids);
        if (sortKeyLookup != null) {
            Map<UUID, Long> sortKeys = sortKeyLookup.apply(ids);
            sortedIds.sort(
                    Comparator.comparingLong((UUID id) -> sortKeys.getOrDefault(id, 0L)).reversed()
                            .thenComparing(Comparator.naturalOrder())
            );
        } else {
            sortedIds.sort(UUID::compareTo);
        }

        int total = sortedIds.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);

        if (start >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        List<UUID> pageIds = sortedIds.subList(start, end);
        List<T> content = findAllById.apply(pageIds);

        if (idExtractor != null) {
            Map<UUID, T> byId = content.stream().collect(Collectors.toMap(idExtractor, Function.identity()));
            content = pageIds.stream().map(byId::get).filter(Objects::nonNull).toList();
        }

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * Intersects IDs from all query terms.
     * <p>
     * Each term runs its own query with an independent tag join, then results
     * are combined with AND logic by retaining only IDs present in every term.
     */
    private Set<UUID> intersectIds(
            List<String> queries,
            Function<String, List<UUID>> searchIdsByQuery
    ) {
        if (queries == null || queries.isEmpty()) {
            return Set.of();
        }

        // Fetch each term's IDs into a mutable HashSet, then intersect pairwise via reduce
        return queries.stream()
                .map(searchIdsByQuery)
                .map(HashSet::new)
                .reduce((a, b) -> {
                    a.retainAll(b);
                    return a;
                })
                .orElseGet(HashSet::new);
    }
}
