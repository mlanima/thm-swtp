package de.thm.swtp.api.search.service;

import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.projectFavorite.repository.ProjectFavoriteRepository;
import de.thm.swtp.api.search.repository.ProjectSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
@Slf4j
@RequiredArgsConstructor
public class ProjectSearchService {

    private final ProjectSearchRepository projectSearchRepository;
    private final ProjectFavoriteRepository projectFavoriteRepository;
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
        List<ProjectEntity> result = searchService.search(
                queries,
                projectSearchRepository::searchIdsByQuery,
                projectSearchRepository::findAllWithTagsById
        );
        log.debug("Project search: queries={}, hits={}", queries, result.size());
        return result;
    }

    /**
     * Searches for projects matching all of the given query terms,
     * with pagination support.
     * <p>
     * Each term is matched case-insensitively against the project name
     * and its tags. Only non-deleted projects are considered.
     * Multiple terms are combined with AND logic. Results are ordered by
     * favorite count, most favorited first.
     *
     * @param queries  one or more search terms
     * @param pageable pagination and sorting information
     * @return a {@link Page} of matching {@link ProjectEntity} instances
     */
    @Transactional(readOnly = true)
    public Page<ProjectEntity> searchProjects(List<String> queries, Pageable pageable) {
        Page<ProjectEntity> page = searchService.search(
                queries,
                projectSearchRepository::searchIdsByQuery,
                projectSearchRepository::findAllWithTagsById,
                pageable,
                this::favoriteCountsByProjectId,
                ProjectEntity::getId
        );
        log.debug("Project search (paged): queries={}, hits={}, page={}/{}",
                queries, page.getNumberOfElements(), pageable.getPageNumber(), page.getTotalPages());
        return page;
    }

    private Map<UUID, Long> favoriteCountsByProjectId(Collection<UUID> projectIds) {
        return projectFavoriteRepository.countByProjectIdIn(projectIds).stream()
                .collect(Collectors.toMap(
                        ProjectFavoriteRepository.ProjectFavoriteCount::getProjectId,
                        ProjectFavoriteRepository.ProjectFavoriteCount::getFavoriteCount
                ));
    }
}
