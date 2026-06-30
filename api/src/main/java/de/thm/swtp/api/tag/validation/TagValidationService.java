package de.thm.swtp.api.tag.validation;

import de.thm.swtp.api.common.LogSafe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TagValidationService {

    private final TagSource tagSource;

    public TagValidationService(final TagSource tagSource) {
        this.tagSource = tagSource;
    }

    @Cacheable(value = "tag-exists", key = "#tagName.toLowerCase()", unless = "#result")
    public boolean isValidTag(final String tagName) {
        log.debug("Cache miss for tag '{}' \u2014 querying source", LogSafe.clean(tagName));
        return tagSource.tagExists(tagName);
    }
}
