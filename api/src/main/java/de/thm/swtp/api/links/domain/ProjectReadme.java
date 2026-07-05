package de.thm.swtp.api.links.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectReadme {

    private String repoUrl;
    private String content;
    private String owner;
    private String repo;
}
