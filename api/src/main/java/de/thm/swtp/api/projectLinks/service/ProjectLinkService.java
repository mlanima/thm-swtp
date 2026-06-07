package de.thm.swtp.api.projectLinks.service;

import de.thm.swtp.api.exceptionhandling.exceptions.ProjectLinkAlreadyExistsException;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectLinkDoesNotBelongToProjectException;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectLinkNotFoundException;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ExceptionProjectEditNotAllowed;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import de.thm.swtp.api.projectLinks.repository.ProjectLinkRepository;
import de.thm.swtp.api.projectLinks.domain.ProjectLink;
import de.thm.swtp.api.projectLinks.entity.ProjectLinkEntity;
import de.thm.swtp.api.projectLinks.mapper.ProjectLinkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectLinkService {
    private final ProjectLinkRepository projectLinkRepository;
    private final ProjectRepository projectRepository;



    @Transactional(readOnly = true)
    public List<ProjectLink> getProjectLinks(UUID projectId){
        getProjectOrThrowError(projectId);

        return projectLinkRepository.findByProjectIdOrderByCreatedAtAsc(projectId)
                .stream()
                .map(ProjectLinkMapper::toDomain)
                .toList();
    }

    @Transactional
    public ProjectLink createProjectLink(UUID projectId, UUID currentUserId, String label, String url){
        ProjectEntity projectEntity = getProjectOrThrowError(projectId);
        checkProjectOwner(projectEntity, currentUserId);

        if (projectLinkRepository.existsByProjectIdAndUrlIgnoreCase(projectEntity.getId(), url)) {
            throw new ProjectLinkAlreadyExistsException();
        }

        ProjectLinkEntity projectLinkEntity = ProjectLinkEntity.builder()
                .project(projectEntity)
                .label(label)
                .url(url)
                .build();

        ProjectLinkEntity saved = projectLinkRepository.save(projectLinkEntity);
        return ProjectLinkMapper.toDomain(saved);
    }


    @Transactional
    public ProjectLink updateProjectLink(UUID projectId, UUID linkId, UUID currentUserId, String label, String url){
        ProjectEntity projectEntity = getProjectOrThrowError(projectId);
        checkProjectOwner(projectEntity, currentUserId);

        ProjectLinkEntity projectLinkEntity = getProjectLinkOrThrowError(linkId);

        if (label != null) {
            projectLinkEntity.setLabel(label.trim());
        }

        if (url != null) {
            String cleanedUrl = url.trim();

            boolean changedUrl = !projectLinkEntity.getUrl().equals(cleanedUrl);

            if (changedUrl && projectLinkRepository.existsByProjectIdAndUrlIgnoreCase(projectEntity.getId(), cleanedUrl)) {
                throw new ProjectLinkAlreadyExistsException();
            }

            projectLinkEntity.setUrl(cleanedUrl);
        }

        ProjectLinkEntity saved = projectLinkRepository.save(projectLinkEntity);
        return ProjectLinkMapper.toDomain(saved);
    }

    @Transactional
    public void deleteProjectLink(UUID projectId, UUID currentUserId, UUID linkId){
        ProjectEntity projectEntity = getProjectOrThrowError(projectId);
        checkProjectOwner(projectEntity, currentUserId);

        ProjectLinkEntity projectLinkEntity = getProjectLinkOrThrowError(linkId);
        checkLinkBelongsToProject(projectLinkEntity, projectId);
        projectLinkRepository.delete(projectLinkEntity);
    }


    private void checkProjectOwner(ProjectEntity projectEntity, UUID currentUserId){
        UUID ownerId = projectEntity.getOwner().getKeycloakId();

        if (!ownerId.equals(currentUserId)){
            throw new ExceptionProjectEditNotAllowed(currentUserId, projectEntity.getId());
        }
    }

    private ProjectEntity getProjectOrThrowError(UUID projectId){
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private ProjectLinkEntity getProjectLinkOrThrowError(UUID linkId){
        return projectLinkRepository.findById(linkId)
                .orElseThrow(() -> new ProjectLinkNotFoundException(linkId));
    }

    private void checkLinkBelongsToProject(ProjectLinkEntity projectLinkEntity, UUID projectId){
        if (!projectLinkEntity.getProject().getId().equals(projectId)){
            throw new ProjectLinkDoesNotBelongToProjectException();
        }
    }
}
