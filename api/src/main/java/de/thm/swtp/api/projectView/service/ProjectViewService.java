package de.thm.swtp.api.projectView.service;

import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.projectView.entity.ProjectViewEntity;
import de.thm.swtp.api.projectView.repository.ProjectViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectViewService {

    private final ProjectViewRepository projectViewRepository;

    @Transactional
    public void addView(ProjectEntity project) {
        projectViewRepository.save(
                ProjectViewEntity.builder()
                        .project(project)
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public long countViews(UUID projectId) {
        return projectViewRepository.countByProjectId(projectId);
    }
}