package de.thm.swtp.api.projectGithubRepo.domain;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class LanguageShare {

    String name;
    double percentage;
}
