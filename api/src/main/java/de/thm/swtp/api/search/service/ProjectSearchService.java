package de.thm.swtp.api.search.service;

import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.search.repository.ProjectSearchRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for searching projects by name or assigned tags.
 * <p>
 * Delegates the actual multi-term intersection and pagination logic
 * to {@link SearchService}. The repository method references wire the
 * generic engine to the project entity type.
 */
@Service
@RequiredArgsConstructor
public class ProjectSearchService {

    private final ProjectSearchRepository projectSearchRepository;
    private final SearchService searchService;

    /**
     * Searches for projects matching all of the given query terms.
     * <p>
     * Each term is matched case-insensitively against the project name
     * and its tags. Only non-deleted projects are considered.
     * Multiple terms are combined with AND logic.
     *
     * @param queries one or more search terms
     * @return list of matching {@link ProjectEntity} instances, or empty list if none
     */
    @Transactional(readOnly = true)
    public List<ProjectEntity> searchProjects(List<String> queries) {
        return searchService.search(
                queries,
                projectSearchRepository::searchIdsByQuery,
                projectSearchRepository::findAllWithTagsById
        );
    }

    /**
     * Searches for projects matching all of the given query terms,
     * with pagination support.
     * <p>
     * Each term is matched case-insensitively against the project name
     * and its tags. Only non-deleted projects are considered.
     * Multiple terms are combined with AND logic.
     *
     * @param queries  one or more search terms
     * @param pageable pagination and sorting information
     * @return a {@link Page} of matching {@link ProjectEntity} instances
     */
    @Transactional(readOnly = true)
    public Page<ProjectEntity> searchProjects(List<String> queries, Pageable pageable) {
        return searchService.search(
                queries,
                projectSearchRepository::searchIdsByQuery,
                projectSearchRepository::findAllWithTagsById,
                pageable
        );
    }
}
