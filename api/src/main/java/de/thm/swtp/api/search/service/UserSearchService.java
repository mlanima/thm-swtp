package de.thm.swtp.api.search.service;

import de.thm.swtp.api.search.dto.UserSearchFilter;
import de.thm.swtp.api.search.repository.UserSearchRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for searching user profiles by username or assigned tags.
 * <p>
 * Delegates the actual multi-term intersection and pagination logic
 * to {@link SearchService}. The repository method references wire the
 * generic engine to the user entity type.
 */
@Service
@RequiredArgsConstructor
public class UserSearchService {

    private final UserSearchRepository userSearchRepository;
    private final SearchService searchService;

    /**
     * Searches for user profiles matching all of the given query terms.
     * <p>
     * Each term is matched case-insensitively against the username
     * and its tags. Multiple terms are combined with AND logic.
     *
     * @param queries one or more search terms
     * @return list of matching {@link UserProfile} instances, or empty list if none
     */
    @Transactional(readOnly = true)
    public List<UserProfile> searchUsers(List<String> queries) {
        return searchService.search(
                queries,
                idSearchFunction(UserSearchFilter.EMPTY),
                userSearchRepository::findAllWithTagsById
        );
    }

    /**
     * Searches for user profiles matching all of the given query terms,
     * with pagination support.
     * <p>
     * Each term is matched case-insensitively against the username
     * and its tags. Multiple terms are combined with AND logic.
     *
     * @param queries  one or more search terms
     * @param pageable pagination and sorting information
     * @return a {@link Page} of matching {@link UserProfile} instances
     */
    @Transactional(readOnly = true)
    public Page<UserProfile> searchUsers(List<String> queries, Pageable pageable) {
        return searchUsers(queries, UserSearchFilter.EMPTY, pageable);
    }

    /**
     * Searches for user profiles matching all of the given query terms and filter criteria,
     * with pagination support.
     * <p>
     * Each term is matched case-insensitively against the username
     * and its tags. Multiple terms are combined with AND logic; the filter is
     * applied on top of every term's query. Banned users are always excluded.
     *
     * @param queries  one or more search terms
     * @param filter   optional filter criteria; use {@link UserSearchFilter#EMPTY} for none
     * @param pageable pagination and sorting information
     * @return a {@link Page} of matching {@link UserProfile} instances
     */
    @Transactional(readOnly = true)
    public Page<UserProfile> searchUsers(List<String> queries, UserSearchFilter filter, Pageable pageable) {
        return searchService.search(
                queries,
                idSearchFunction(filter),
                userSearchRepository::findAllWithTagsById,
                pageable
        );
    }

    private Function<String, List<UUID>> idSearchFunction(UserSearchFilter filter) {
        List<String> tags = filter.tags() == null ? List.of() : filter.tags();
        return term -> userSearchRepository.searchIdsByQuery(
                term,
                filter.isProfessor(),
                tags,
                tags.size(),
                filter.location(),
                filter.createdAfter(),
                filter.createdBefore()
        );
    }
}
