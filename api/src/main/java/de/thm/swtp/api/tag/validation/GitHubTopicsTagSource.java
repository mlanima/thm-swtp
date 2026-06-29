package de.thm.swtp.api.tag.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.tags.source", havingValue = "github")
public class GitHubTopicsTagSource implements TagSource {

    private final GitHubTopicsClient gitHubTopicsClient;

    public GitHubTopicsTagSource(final GitHubTopicsClient gitHubTopicsClient) {
        this.gitHubTopicsClient = gitHubTopicsClient;
    }

    @Override
    public boolean tagExists(final String tagName) {
        return gitHubTopicsClient.tagExists(tagName);
    }
}
