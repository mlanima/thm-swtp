package de.thm.swtp.api.tag.profile;

import de.thm.swtp.api.tag.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileTagService {

    private final TagRepository tagRepository;
}
