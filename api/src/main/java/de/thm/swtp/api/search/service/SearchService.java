package de.thm.swtp.api.search.service;

import de.thm.swtp.api.search.dto.ProjectSearchResult;
import de.thm.swtp.api.search.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchRepository searchRepository;

    @Transactional(readOnly = true)
    public List<ProjectSearchResult> searchProjects(String query) {
        return searchRepository.findByNameContainingIgnoreCase(query)
                .stream()
                .map(p -> ProjectSearchResult.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .build())
                .toList();
    }
}
