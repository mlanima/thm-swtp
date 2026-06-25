package de.thm.swtp.api.tag.service;

import de.thm.swtp.api.tag.domain.Tag;
import de.thm.swtp.api.tag.mapper.TagMapper;
import de.thm.swtp.api.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    @Transactional(readOnly = true)
    public List<Tag> searchTags(String query, int limit) {
        if (query == null || query.isBlank()) {
            return tagRepository.findPopularTags(PageRequest.of(0, limit))
                    .stream()
                    .map(TagMapper::toDomain)
                    .toList();
        }
        return tagRepository.findByNameContainingIgnoreCase(query.trim())
                .stream()
                .limit(limit)
                .map(TagMapper::toDomain)
                .toList();
    }
}
