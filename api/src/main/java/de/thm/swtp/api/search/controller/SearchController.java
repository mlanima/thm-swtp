package de.thm.swtp.api.search.controller;

import de.thm.swtp.api.search.dto.ProjectSearchResult;
import de.thm.swtp.api.search.service.SearchService;
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

    private final SearchService searchService;

    @GetMapping("/projects")
    public List<ProjectSearchResult> searchProjects(@RequestParam String q) {
        return searchService.searchProjects(q);
    }
}
