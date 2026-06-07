package de.thm.swtp.api.search.service;

import de.thm.swtp.api.search.repository.UserSearchRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import lombok.RequiredArgsConstructor;

import java.util.List;
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
                userSearchRepository::searchIdsByQuery,
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
        return searchService.search(
                queries,
                userSearchRepository::searchIdsByQuery,
                userSearchRepository::findAllWithTagsById,
                pageable
        );
    }
}
