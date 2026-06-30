package de.thm.swtp.api.tag.validation;

import de.thm.swtp.api.common.LogSafe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(name = "app.tags.source", havingValue = "openai-github")
public class OpenAIModeratedGithubTagSource implements TagSource {

    private final OpenAIModerationClient moderationClient;
    private final BlocklistService blocklistService;
    private final GitHubTopicsClient gitHubTopicsClient;

    @Override
    public boolean tagExists(final String tagName) {
        var cleaned = tagName.toLowerCase().trim();

        try {
            if (moderationClient.isFlagged(cleaned)) {
                log.warn("Tag flagged by OpenAI moderation: {}", LogSafe.clean(tagName));
                return false;
            }
        } catch (TagValidationException e) {
            log.warn("OpenAI moderation unavailable, falling back to blocklist: {}",
                    LogSafe.clean(tagName));
            if (blocklistService.contains(cleaned)) {
                log.warn("Blocked tag rejected (fallback): {}", LogSafe.clean(tagName));
                return false;
            }
            log.debug("Blocklist passed, querying GitHub for tag: {}", LogSafe.clean(tagName));
        }

        return gitHubTopicsClient.tagExists(cleaned);
    }
}
