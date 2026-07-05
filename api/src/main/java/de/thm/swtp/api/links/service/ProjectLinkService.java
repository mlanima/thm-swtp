package de.thm.swtp.api.links.service;

import de.thm.swtp.api.exceptionhandling.exceptions.LinkIsNotGitHubRepositoryException;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectLinkAlreadyExistsException;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectLinkDoesNotBelongToProjectException;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectLinkNotFoundException;
import de.thm.swtp.api.links.domain.LinkVisibility;
import de.thm.swtp.api.links.domain.ProjectReadme;
import de.thm.swtp.api.links.github.GitHubReadmeService;
import de.thm.swtp.api.links.github.GitHubRepoRef;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import de.thm.swtp.api.links.repository.ProjectLinkRepository;
import de.thm.swtp.api.links.domain.ProjectLink;
import de.thm.swtp.api.links.entity.ProjectLinkEntity;
import de.thm.swtp.api.links.mapper.ProjectLinkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectLinkService {
    private final ProjectLinkRepository projectLinkRepository;
    private final ProjectRepository projectRepository;
    private final GitHubReadmeService gitHubReadmeService;



    @Transactional(readOnly = true)
    public List<ProjectLink> getProjectLinks(UUID projectId, UUID currentUserId){
        ProjectEntity projectEntity = getProjectOrThrowError(projectId);

        boolean allowedToSeePrivateLinks = isProjectOwner(projectEntity, currentUserId) || isProjectMember(projectEntity, currentUserId);

        if (allowedToSeePrivateLinks) {
            return projectLinkRepository.findByProjectIdOrderByCreatedAtAsc(projectId)
                    .stream()
                    .map(ProjectLinkMapper::toDomain)
                    .toList();
        }

        return projectLinkRepository.findByProjectIdAndVisibilityOrderByCreatedAtAsc(projectId, LinkVisibility.PUBLIC)
                .stream()
                .map(ProjectLinkMapper::toDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ProjectReadme> getProjectReadme(UUID projectId, UUID currentUserId){
        return getProjectLinks(projectId, currentUserId).stream()
                .filter(ProjectLink::isShowReadme)
                .findFirst()
                .flatMap(link -> GitHubRepoRef.parse(link.getUrl())
                        .flatMap(repoRef -> gitHubReadmeService.getReadme(repoRef.owner(), repoRef.repo())
                                .map(content -> ProjectReadme.builder()
                                        .repoUrl(link.getUrl())
                                        .content(content)
                                        .owner(repoRef.owner())
                                        .repo(repoRef.repo())
                                        .build())));
    }

    @Transactional
    public ProjectLink createProjectLink(UUID projectId, String label, String url, LinkVisibility visibility){
        ProjectEntity projectEntity = getProjectOrThrowError(projectId);

        String cleanedLabel = label.trim();
        String cleanedUrl = url.trim();
        LinkVisibility cleanedVisibility = visibility != null ? visibility : LinkVisibility.PUBLIC;

        if (projectLinkRepository.existsByProjectIdAndUrlIgnoreCase(projectEntity.getId(), cleanedUrl)) {
            throw new ProjectLinkAlreadyExistsException();
        }



        ProjectLinkEntity projectLinkEntity = ProjectLinkEntity.builder()
                .project(projectEntity)
                .label(cleanedLabel)
                .url(cleanedUrl)
                .visibility(cleanedVisibility)
                .build();

        ProjectLinkEntity saved = projectLinkRepository.save(projectLinkEntity);
        return ProjectLinkMapper.toDomain(saved);
    }


    @Transactional
    public ProjectLink updateProjectLink(UUID projectId, UUID linkId, String label, String url, LinkVisibility visibility, Boolean showReadme){
        ProjectEntity projectEntity = getProjectOrThrowError(projectId);

        ProjectLinkEntity projectLinkEntity = getProjectLinkOrThrowError(linkId);
        checkLinkBelongsToProject(projectLinkEntity, projectId);

        if (label != null) {
            projectLinkEntity.setLabel(label.trim());
        }

        if (url != null) {
            String cleanedUrl = url.trim();

            boolean changedUrl = !projectLinkEntity.getUrl().equalsIgnoreCase(cleanedUrl);

            if (changedUrl && projectLinkRepository.existsByProjectIdAndUrlIgnoreCase(projectEntity.getId(), cleanedUrl)) {
                throw new ProjectLinkAlreadyExistsException();
            }

            projectLinkEntity.setUrl(cleanedUrl);
        }

        if (visibility != null) {
            projectLinkEntity.setVisibility(visibility);
        }

        if (showReadme != null) {
            applyShowReadme(projectEntity, projectLinkEntity, showReadme);
        }

        ProjectLinkEntity saved = projectLinkRepository.save(projectLinkEntity);
        return ProjectLinkMapper.toDomain(saved);
    }

    private void applyShowReadme(ProjectEntity projectEntity, ProjectLinkEntity projectLinkEntity, boolean showReadme){
        if (!showReadme) {
            projectLinkEntity.setShowReadme(false);
            return;
        }

        if (GitHubRepoRef.parse(projectLinkEntity.getUrl()).isEmpty()) {
            throw new LinkIsNotGitHubRepositoryException();
        }

        List<ProjectLinkEntity> siblingsWithReadmeShown = projectLinkRepository
                .findByProjectIdAndShowReadmeTrueAndIdNot(projectEntity.getId(), projectLinkEntity.getId());

        siblingsWithReadmeShown.forEach(sibling -> sibling.setShowReadme(false));
        projectLinkRepository.saveAll(siblingsWithReadmeShown);

        projectLinkEntity.setShowReadme(true);
    }

    @Transactional
    public void deleteProjectLink(UUID projectId, UUID linkId){
        getProjectOrThrowError(projectId);

        ProjectLinkEntity projectLinkEntity = getProjectLinkOrThrowError(linkId);
        checkLinkBelongsToProject(projectLinkEntity, projectId);
        projectLinkRepository.delete(projectLinkEntity);
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

    private boolean isProjectOwner(ProjectEntity projectEntity, UUID currentUserId){
        return projectEntity.getOwner().getKeycloakId().equals(currentUserId);
    }

    private boolean isProjectMember(ProjectEntity projectEntity, UUID currentUserId){
        return projectEntity.getMembers()
                .stream()
                .anyMatch(member -> member.getKeycloakId().equals(currentUserId));
    }
}
