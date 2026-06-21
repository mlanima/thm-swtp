package de.thm.swtp.api.projectPost.dto;

import de.thm.swtp.api.projectPost.domain.PostContentFormat;
import de.thm.swtp.api.projectPost.domain.ProjectPostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProjectPostRequest(@NotBlank @Size(max = 200) String title,
                                       @NotBlank @Size(max = 10000) String content,
                                       @NotNull PostContentFormat contentFormat,
                                       @NotNull ProjectPostStatus status) {
}
