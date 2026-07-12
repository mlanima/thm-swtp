package de.thm.swtp.api.discord.stream;

import de.thm.swtp.api.projectPost.entity.ProjectPostEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PostUrlBuilder {

    private final String frontendUrl;

    public PostUrlBuilder(@Value("${app.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    public String buildPostUrl(ProjectPostEntity post) {
        return frontendUrl + "/project/" + post.getProject().getProjectUrl();
    }
}
