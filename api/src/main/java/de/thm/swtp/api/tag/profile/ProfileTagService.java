package de.thm.swtp.api.tag.profile;

import de.thm.swtp.api.profile.Profile;
import de.thm.swtp.api.profile.ProfileRepository;
import de.thm.swtp.api.tag.TagRepository;
import de.thm.swtp.api.tag.dto.AddProfileTagRequest;
import de.thm.swtp.api.tag.entity.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProfileTagService {

    private final TagRepository tagRepository;
    private final ProfileRepository profileTagRepository;

    @Transactional
    public Tag addTagToProfile(String profileId, AddProfileTagRequest request) {
        Profile profile = profileTagRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));

        String name = request.getName().trim();
        Tag tag = tagRepository.findByName(name)
                .orElseGet(() -> tagRepository.save(new Tag(name)));

        profile.getTags().add(tag);

        return tag;
    }
}
