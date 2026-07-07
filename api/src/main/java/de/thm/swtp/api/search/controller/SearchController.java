package de.thm.swtp.api.search.controller;

import de.thm.swtp.api.search.dto.ProjectSearchFilter;
import de.thm.swtp.api.search.dto.ProjectSearchResult;
import de.thm.swtp.api.search.dto.UserSearchFilter;
import de.thm.swtp.api.search.dto.UserSearchResult;
import de.thm.swtp.api.search.mapper.ProjectSearchMapper;
import de.thm.swtp.api.search.mapper.UserSearchMapper;
import de.thm.swtp.api.search.service.ProjectSearchService;
import de.thm.swtp.api.search.service.UserSearchService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/search")
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
     * @deprecated Use {@link #searchProjectsPaged(List, Pageable)} instead.
     */
    @Deprecated
    // @GetMapping("/projects")
    public List<ProjectSearchResult> searchProjects(@RequestParam List<String> q) {
        return projectSearchService.searchProjects(q)
                .stream()
                .map(projectSearchMapper::toResponse)
                .toList();
    }

    /**
     * Searches projects by name or tags with pagination support.
     * All filter parameters are optional and narrow the keyword search results.
     *
     * @param q                  one or more search terms (AND logic across terms)
     * @param hasOpenPositions   restrict to projects with (or without) open positions
     * @param allowJoinRequests  restrict to projects accepting (or not) join requests
     * @param tags               restrict to projects having at least one of these tags
     * @param createdAfter       restrict to projects created at or after this timestamp
     * @param createdBefore      restrict to projects created at or before this timestamp
     * @param pageable           pagination parameters (default: page 0, size 20)
     * @return a {@link Page} of project search results
     */
    @GetMapping("/projects/paged")
    public Page<ProjectSearchResult> searchProjectsPaged(
            @RequestParam List<String> q,
            @RequestParam(required = false) Boolean hasOpenPositions,
            @RequestParam(required = false) Boolean allowJoinRequests,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        ProjectSearchFilter filter = new ProjectSearchFilter(
                hasOpenPositions, allowJoinRequests, tags, createdAfter, createdBefore
        );
        return projectSearchService.searchProjects(q, filter, pageable)
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
     * All filter parameters are optional and narrow the keyword search results.
     * Banned users are always excluded.
     *
     * @param q             one or more search terms (AND logic across terms)
     * @param isProfessor   restrict to verified professors (or non-professors)
     * @param tags          restrict to users having at least one of these tags
     * @param location      restrict to users whose location contains this text
     * @param createdAfter  restrict to users created at or after this timestamp
     * @param createdBefore restrict to users created at or before this timestamp
     * @param pageable      pagination parameters (default: page 0, size 20)
     * @return a {@link Page} of user search results
     */
    @GetMapping("/users/paged")
    public Page<UserSearchResult> searchUsersPaged(
            @RequestParam List<String> q,
            @RequestParam(required = false) Boolean isProfessor,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UserSearchFilter filter = new UserSearchFilter(
                isProfessor, tags, location, createdAfter, createdBefore
        );
        return userSearchService.searchUsers(q, filter, pageable)
                .map(userSearchMapper::toResponse);
    }
}
