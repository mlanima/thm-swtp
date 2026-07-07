package de.thm.swtp.api.search.service;

import de.thm.swtp.api.search.repository.UserSearchRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserSearchServiceTest {

    private UserSearchRepository repository;
    private UserSearchService service;

    private UUID id1;
    private UUID id2;
    private UserProfile user1;
    private UserProfile user2;

    @BeforeEach
    void setup() {
        repository = mock(UserSearchRepository.class);
        service = new UserSearchService(repository, new SearchService());

        id1 = UUID.randomUUID();
        id2 = UUID.randomUUID();

        user1 = new UserProfile();
        user1.setKeycloakId(id1);

        user2 = new UserProfile();
        user2.setKeycloakId(id2);
    }

    @Test
    void searchUsers_shouldReturnAll_whenSingleQuery() {
        when(repository.searchIdsByQuery("john")).thenReturn(List.of(id1, id2));
        when(repository.findAllWithTagsById(Set.of(id1, id2))).thenReturn(List.of(user1, user2));

        List<UserProfile> result = service.searchUsers(List.of("john"));

        assertThat(result).containsExactlyInAnyOrder(user1, user2);
    }

    @Test
    void searchUsers_shouldIntersect_whenMultipleQueries() {
        when(repository.searchIdsByQuery("john")).thenReturn(List.of(id1, id2));
        when(repository.searchIdsByQuery("doe")).thenReturn(List.of(id2));
        when(repository.findAllWithTagsById(Set.of(id2))).thenReturn(List.of(user2));

        List<UserProfile> result = service.searchUsers(List.of("john", "doe"));

        assertThat(result).containsExactly(user2);
    }

    @Test
    void searchUsers_shouldReturnEmpty_whenNoMatch() {
        when(repository.searchIdsByQuery("john")).thenReturn(List.of());

        List<UserProfile> result = service.searchUsers(List.of("john"));

        assertThat(result).isEmpty();
    }

    @Test
    void searchUsers_shouldReturnEmpty_whenQueriesEmpty() {
        List<UserProfile> result = service.searchUsers(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void searchUsersPaged_shouldReturnPage() {
        when(repository.searchIdsByQuery("john")).thenReturn(List.of(id1, id2));
        when(repository.findAllWithTagsById(any())).thenReturn(List.of(user1));

        var page = service.searchUsers(List.of("john"), PageRequest.of(0, 1));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}
