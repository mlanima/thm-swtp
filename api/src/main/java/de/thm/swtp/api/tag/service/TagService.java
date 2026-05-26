package de.thm.swtp.api.tag.service;

import de.thm.swtp.api.tag.domain.Tag;
import de.thm.swtp.api.tag.mapper.TagMapper;
import de.thm.swtp.api.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Service for managing {@link Tag} domain objects.*/
@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;

    /** Returns a list of all tags.*/
    @Transactional(readOnly = true)
    public List<Tag> getTags(){
        return tagRepository.findAll()
                .stream()
                .map(TagMapper::toDomain)
                .toList();
    }

    /** Returns a list of all tags filtered by the given name. Name must match.*/
    @Transactional(readOnly = true)
    public List<Tag> getTagsName(String name){
        return tagRepository.findByName(name)
                .stream()
                .map(TagMapper::toDomain)
                .toList();
    }

    /** Returns a list of tags that partially match the search value.
     * If the search value is empty, then all tags are returned.
     */
    @Transactional(readOnly = true)
    public List<Tag> searchForTags(String search){
        if (search == null || search.isEmpty()){
            return getTags();
        }

        return tagRepository.findByNameContainingIgnoreCase(search.trim())
                .stream()
                .map(TagMapper::toDomain)
                .toList();
    }

    /** Returns a list of tags belonging to a project.*/
    @Transactional(readOnly = true)
    public List<Tag> getProjectTags(){
        return tagRepository.findDistinctByProjectsIsNotEmpty()
                .stream()
                .map(TagMapper::toDomain)
                .toList();
    }

    /** Returns a list of tags belonging to a user profile.*/
    @Transactional(readOnly = true)
    public List<Tag> getUserProfileTags(){
        return tagRepository.findDistinctByUserProfilesIsNotEmpty()
                .stream()
                .map(TagMapper::toDomain)
                .toList();
    }
}
