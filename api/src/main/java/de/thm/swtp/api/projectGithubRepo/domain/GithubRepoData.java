package de.thm.swtp.api.projectGithubRepo.domain;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class GithubRepoData {

    String fullName;
    String htmlUrl;
    String description;
    int stargazersCount;
    int forksCount;
    List<LanguageShare> languages;
}
