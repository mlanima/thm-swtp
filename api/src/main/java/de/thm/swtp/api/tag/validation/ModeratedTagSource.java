package de.thm.swtp.api.tag.validation;

import de.thm.swtp.api.common.LogSafe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(name = "app.tags.source", havingValue = "github-blocklist", matchIfMissing = true)
public class ModeratedTagSource implements TagSource {

    private final BlocklistService blocklistService;
    private final GitHubTopicsClient gitHubTopicsClient;

    @Override
    public boolean tagExists(final String tagName) {
        var cleaned = tagName.toLowerCase().trim();

        if (blocklistService.contains(cleaned)) {
            log.warn("Blocked tag rejected: {}", LogSafe.clean(tagName));
            return false;
        }

        return gitHubTopicsClient.tagExists(cleaned);
    }
}
