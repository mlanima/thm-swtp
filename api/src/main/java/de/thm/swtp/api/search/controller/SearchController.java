package de.thm.swtp.api.search.controller;

import de.thm.swtp.api.search.dto.ProjectSearchResult;
import de.thm.swtp.api.search.dto.UserSearchResult;
import de.thm.swtp.api.search.mapper.ProjectSearchMapper;
import de.thm.swtp.api.search.mapper.UserSearchMapper;
import de.thm.swtp.api.search.service.ProjectSearchService;
import de.thm.swtp.api.search.service.UserSearchService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing search endpoints for projects and users.
 * <p>
 * Supports multi-term search via the {@code q} query parameter.
 * Multiple values for {@code q} are combined with AND logic.
 * <p>
 * Examples:
 * <ul>
 *   <li>{@code GET /api/search/projects?q=web&q=java} — all results</li>
 *   <li>{@code GET /api/search/projects/paged?q=web&q=java&page=0&size=10} — paginated</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProjectSearchService projectSearchService;
    private final UserSearchService userSearchService;
    private final ProjectSearchMapper projectSearchMapper;
    private final UserSearchMapper userSearchMapper;

    /**
     * Searches projects by name or tags. Returns all matching results.
     *
     * @param q one or more search terms (AND logic across terms)
     * @return list of project search results
     */
    @GetMapping("/projects")
    public List<ProjectSearchResult> searchProjects(@RequestParam List<String> q) {
        return projectSearchService.searchProjects(q)
                .stream()
                .map(projectSearchMapper::toResponse)
                .toList();
    }

    /**
     * Searches projects by name or tags with pagination support.
     *
     * @param q        one or more search terms (AND logic across terms)
     * @param pageable pagination parameters (default: page 0, size 20)
     * @return a {@link Page} of project search results
     */
    @GetMapping("/projects/paged")
    public Page<ProjectSearchResult> searchProjectsPaged(
            @RequestParam List<String> q,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return projectSearchService.searchProjects(q, pageable)
                .map(projectSearchMapper::toResponse);
    }

    /**
     * Searches users by username or tags. Returns all matching results.
     *
     * @param q one or more search terms (AND logic across terms)
     * @return list of user search results
     */
    @GetMapping("/users")
    public List<UserSearchResult> searchUsers(@RequestParam List<String> q) {
        return userSearchService.searchUsers(q)
                .stream()
                .map(userSearchMapper::toResponse)
                .toList();
    }

    /**
     * Searches users by username or tags with pagination support.
     *
     * @param q        one or more search terms (AND logic across terms)
     * @param pageable pagination parameters (default: page 0, size 20)
     * @return a {@link Page} of user search results
     */
    @GetMapping("/users/paged")
    public Page<UserSearchResult> searchUsersPaged(
            @RequestParam List<String> q,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return userSearchService.searchUsers(q, pageable)
                .map(userSearchMapper::toResponse);
    }
}
