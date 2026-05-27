package de.thm.swtp.api.search.service;

import de.thm.swtp.api.search.dto.ProjectSearchResult;
import de.thm.swtp.api.search.repository.ProjectSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectSearchService {

    private final ProjectSearchRepository projectSearchRepository;

    // TODO: no pagination, it returns all matches at once.
    @Transactional(readOnly = true)
    public List<ProjectSearchResult> searchProjects(String query) {
        return projectSearchRepository.findByNameContainingIgnoreCase(query)
                .stream()
                .map(p -> ProjectSearchResult.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .build())
                .toList();
    }
}
