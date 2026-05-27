package de.thm.swtp.api.search.controller;

import de.thm.swtp.api.search.dto.ProjectSearchResult;
import de.thm.swtp.api.search.dto.UserSearchResult;
import de.thm.swtp.api.search.service.ProjectSearchService;
import de.thm.swtp.api.search.service.UserSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProjectSearchService projectSearchService;
    private final UserSearchService userSearchService;

    @GetMapping("/projects")
    public List<ProjectSearchResult> searchProjects(@RequestParam String q) {
        return projectSearchService.searchProjects(q);
    }

    @GetMapping("/users")
    public List<UserSearchResult> searchUsers(@RequestParam String q) {
        return userSearchService.searchUsers(q);
    }
}
