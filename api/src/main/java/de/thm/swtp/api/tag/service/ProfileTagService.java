package de.thm.swtp.api.tag.service;


import de.thm.swtp.api.tag.domain.Tag;
import de.thm.swtp.api.tag.entity.TagEntity;
import de.thm.swtp.api.tag.mapper.TagMapper;
import de.thm.swtp.api.tag.repository.TagRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileTagService {

    private final TagRepository tagRepository;
    private final UserProfileRepository userProfileRepository;


    /** Returns a list of all tags assigned to the user profile. */
    @Transactional(readOnly = true)
    public List<Tag> getUserProfileTags(UUID userId){
        UserProfile userProfile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId.toString()));

        return userProfile.getTags()
                .stream()
                .map(TagMapper::toDomain)
                .toList();
    }

    /** Assigns a tag to the user profile. If the tag does not exist, then it will be created and then assigned. */
    @Transactional
    public Tag addTagToProfile(UUID userId, String tagName) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId.toString()));

        TagEntity tag = getOrCreateTag(tagName);
        profile.getTags().add(tag);

        return TagMapper.toDomain(tag);
    }

    private TagEntity getOrCreateTag(String tagName) {
        String cleaned = tagName.trim();

        return tagRepository.findByNameIgnoreCase(cleaned)
                .orElseGet(() -> tagRepository.save(new TagEntity(cleaned)));
    }
}
