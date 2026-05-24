package de.thm.swtp.api.tag.project;

import de.thm.swtp.api.tag.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectTagService {

    private final TagRepository tagRepository;

}
