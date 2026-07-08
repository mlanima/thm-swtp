package de.thm.swtp.api.github.dto;

import jakarta.validation.constraints.NotBlank;

public record GithubCallbackRequest(@NotBlank String code, @NotBlank String state) {
}
