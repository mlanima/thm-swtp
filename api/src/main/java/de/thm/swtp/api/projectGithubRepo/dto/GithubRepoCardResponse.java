package de.thm.swtp.api.projectGithubRepo.dto;

import de.thm.swtp.api.projectGithubRepo.domain.GithubRepoCard;
import de.thm.swtp.api.projectGithubRepo.domain.LanguageShare;

import java.util.List;

public record GithubRepoCardResponse(
        String repoOwner,
        String repoName,
        String htmlUrl,
        String description,
        Integer stargazersCount,
        Integer forksCount,
        List<LanguageShareResponse> languages,
        boolean dataUnavailable) {

    public static GithubRepoCardResponse toResponse(GithubRepoCard card) {
        var link = card.getLink();
        var data = card.getData();
        if (data == null) {
            return new GithubRepoCardResponse(
                    link.getRepoOwner(), link.getRepoName(), null, null, null, null, List.of(), true);
        }
        return new GithubRepoCardResponse(
                link.getRepoOwner(),
                link.getRepoName(),
                data.getHtmlUrl(),
                data.getDescription(),
                data.getStargazersCount(),
                data.getForksCount(),
                data.getLanguages().stream().map(LanguageShareResponse::toResponse).toList(),
                false);
    }

    public record LanguageShareResponse(String name, double percentage) {
        public static LanguageShareResponse toResponse(LanguageShare share) {
            return new LanguageShareResponse(share.getName(), share.getPercentage());
        }
    }
}
