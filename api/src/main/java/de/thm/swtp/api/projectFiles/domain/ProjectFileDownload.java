package de.thm.swtp.api.projectFiles.domain;

import org.springframework.core.io.Resource;

public record ProjectFileDownload(ProjectFile file, Resource resource) {
}