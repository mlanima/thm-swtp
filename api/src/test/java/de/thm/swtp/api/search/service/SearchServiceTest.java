package de.thm.swtp.api.search.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class SearchServiceTest {

    private SearchService searchService;

    private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ID_3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @BeforeEach
    void setup() {
        searchService = new SearchService();
    }

    @Test
    void search_shouldReturnEmpty_whenQueriesIsNull() {
        List<String> result = searchService.search(null, q -> List.of(), ids -> List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void search_shouldReturnEmpty_whenQueriesIsEmpty() {
        List<String> result = searchService.search(List.of(), q -> List.of(), ids -> List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void search_shouldReturnAllFromSingleQuery() {
        Function<String, List<UUID>> searchById = q -> switch (q) {
            case "java" -> List.of(ID_1, ID_2);
            default -> List.of();
        };

        List<String> result = searchService.search(List.of("java"), searchById, ids -> List.of("e1", "e2"));

        assertThat(result).containsExactly("e1", "e2");
    }

    @Test
    void search_shouldIntersectMultipleQueries() {
        Function<String, List<UUID>> searchById = q -> switch (q) {
            case "web" -> List.of(ID_1, ID_2);
            case "java" -> List.of(ID_2, ID_3);
            default -> List.of();
        };

        List<String> result = searchService.search(
                List.of("web", "java"),
                searchById,
                ids -> ids.stream().map(UUID::toString).toList()
        );

        assertThat(result).containsExactly(ID_2.toString());
    }

    @Test
    void search_shouldReturnEmpty_whenNoOverlap() {
        Function<String, List<UUID>> searchById = q -> switch (q) {
            case "web" -> List.of(ID_1);
            case "java" -> List.of(ID_2);
            default -> List.of();
        };

        List<String> result = searchService.search(
                List.of("web", "java"),
                searchById,
                ids -> List.of()
        );

        assertThat(result).isEmpty();
    }

    @Test
    void search_shouldReturnEmptyPage_whenQueriesIsNull() {
        var page = searchService.search(null, q -> List.of(), ids -> List.of(), Pageable.ofSize(10));

        assertThat(page).isEmpty();
    }

    @Test
    void search_shouldReturnEmptyPage_whenQueriesIsEmpty() {
        var page = searchService.search(List.of(), q -> List.of(), ids -> List.of(), Pageable.ofSize(10));

        assertThat(page).isEmpty();
    }

    @Test
    void searchPaged_shouldReturnFirstPage() {
        Function<String, List<UUID>> searchById = q -> List.of(ID_1, ID_2, ID_3);

        var page = searchService.search(
                List.of("java"),
                searchById,
                ids -> ids.stream().map(UUID::toString).toList(),
                PageRequest.of(0, 2)
        );

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getNumber()).isZero();
    }

    @Test
    void searchPaged_shouldReturnSecondPage() {
        Function<String, List<UUID>> searchById = q -> List.of(ID_1, ID_2, ID_3);

        var page = searchService.search(
                List.of("java"),
                searchById,
                ids -> ids.stream().map(UUID::toString).toList(),
                PageRequest.of(1, 2)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getNumber()).isEqualTo(1);
    }

    @Test
    void searchPaged_shouldReturnEmptyPage_whenOffsetExceedsTotal() {
        Function<String, List<UUID>> searchById = q -> List.of(ID_1);

        var page = searchService.search(
                List.of("java"),
                searchById,
                ids -> List.of(),
                PageRequest.of(5, 10)
        );

        assertThat(page).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void searchPaged_shouldIntersectAcrossPages() {
        Function<String, List<UUID>> searchById = q -> switch (q) {
            case "web" -> List.of(ID_1, ID_2, ID_3);
            case "java" -> List.of(ID_2, ID_3);
            default -> List.of();
        };

        var page = searchService.search(
                List.of("web", "java"),
                searchById,
                ids -> ids.stream().map(UUID::toString).toList(),
                PageRequest.of(0, 1)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void searchPaged_shouldPreserveTotalCountAcrossPages() {
        Function<String, List<UUID>> allIds = q -> List.of(ID_1, ID_2, ID_3, ID_1);

        var page1 = searchService.search(
                List.of("java"),
                allIds,
                ids -> ids.stream().map(UUID::toString).toList(),
                PageRequest.of(0, 2)
        );

        var page2 = searchService.search(
                List.of("java"),
                allIds,
                ids -> ids.stream().map(UUID::toString).toList(),
                PageRequest.of(1, 2)
        );

        assertThat(page1.getTotalElements()).isEqualTo(3);
        assertThat(page2.getTotalElements()).isEqualTo(3);
        assertThat(page1.getContent()).doesNotContainNull();
        assertThat(page2.getContent()).doesNotContainNull();
    }
}
