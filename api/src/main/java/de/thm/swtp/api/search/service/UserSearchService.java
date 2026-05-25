package de.thm.swtp.api.search.service;

import de.thm.swtp.api.search.dto.UserSearchResult;
import de.thm.swtp.api.search.repository.UserSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSearchService {

    private final UserSearchRepository userSearchRepository;

    // TODO: no pagination, it returns all matches at once.
    @Transactional(readOnly = true)
    public List<UserSearchResult> searchUsers(String query) {
        return userSearchRepository.findByUsernameContainingIgnoreCase(query)
                .stream()
                .map(u -> UserSearchResult.builder()
                        .keycloakId(u.getKeycloakId())
                        .username(u.getUsername())
                        .title(u.getTitle())
                        .location(u.getLocation())
                        .build())
                .toList();
    }
}
