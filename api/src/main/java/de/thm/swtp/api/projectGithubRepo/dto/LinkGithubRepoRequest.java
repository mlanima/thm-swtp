package de.thm.swtp.api.projectGithubRepo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LinkGithubRepoRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{1,100}") String repoOwner,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,150}") String repoName) {
}
