package de.thm.swtp.api.projectFiles.dto;

import de.thm.swtp.api.projectFiles.domain.FileVisibility;
import jakarta.validation.constraints.NotNull;

public record UpdateProjectFileRequest(@NotNull FileVisibility visibility) {
}