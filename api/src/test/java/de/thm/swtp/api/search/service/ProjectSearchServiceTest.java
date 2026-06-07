package de.thm.swtp.api.search.service;

import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.search.repository.ProjectSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProjectSearchServiceTest {

    private ProjectSearchRepository repository;
    private ProjectSearchService service;

    private UUID id1;
    private UUID id2;
    private ProjectEntity project1;
    private ProjectEntity project2;

    @BeforeEach
    void setup() {
        repository = mock(ProjectSearchRepository.class);
        service = new ProjectSearchService(repository, new SearchService());

        id1 = UUID.randomUUID();
        id2 = UUID.randomUUID();

        project1 = new ProjectEntity();
        project1.setId(id1);

        project2 = new ProjectEntity();
        project2.setId(id2);
    }

    @Test
    void searchProjects_shouldReturnAll_whenSingleQuery() {
        when(repository.searchIdsByQuery("java")).thenReturn(List.of(id1, id2));
        when(repository.findAllWithTagsById(Set.of(id1, id2))).thenReturn(List.of(project1, project2));

        List<ProjectEntity> result = service.searchProjects(List.of("java"));

        assertThat(result).containsExactlyInAnyOrder(project1, project2);
    }

    @Test
    void searchProjects_shouldIntersect_whenMultipleQueries() {
        when(repository.searchIdsByQuery("web")).thenReturn(List.of(id1, id2));
        when(repository.searchIdsByQuery("java")).thenReturn(List.of(id2));
        when(repository.findAllWithTagsById(Set.of(id2))).thenReturn(List.of(project2));

        List<ProjectEntity> result = service.searchProjects(List.of("web", "java"));

        assertThat(result).containsExactly(project2);
    }

    @Test
    void searchProjects_shouldReturnEmpty_whenNoMatch() {
        when(repository.searchIdsByQuery("web")).thenReturn(List.of());

        List<ProjectEntity> result = service.searchProjects(List.of("web"));

        assertThat(result).isEmpty();
    }

    @Test
    void searchProjects_shouldReturnEmpty_whenQueriesEmpty() {
        List<ProjectEntity> result = service.searchProjects(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void searchProjectsPaged_shouldReturnPage() {
        when(repository.searchIdsByQuery("java")).thenReturn(List.of(id1, id2));
        when(repository.findAllWithTagsById(any())).thenReturn(List.of(project1));

        var page = service.searchProjects(List.of("java"), PageRequest.of(0, 1));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}
