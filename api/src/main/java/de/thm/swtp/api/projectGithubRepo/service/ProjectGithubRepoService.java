package de.thm.swtp.api.projectGithubRepo.service;

import de.thm.swtp.api.github.exception.GithubConnectionRequiredException;
import de.thm.swtp.api.github.exception.GithubTokenInvalidException;
import de.thm.swtp.api.github.exception.PrivateRepoNotAllowedException;
import de.thm.swtp.api.github.service.GithubConnectionService;
import de.thm.swtp.api.github.client.GithubApiClient;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import de.thm.swtp.api.projectGithubRepo.domain.GithubRepoCard;
import de.thm.swtp.api.projectGithubRepo.domain.GithubRepoLink;
import de.thm.swtp.api.projectGithubRepo.entity.ProjectGithubRepoEntity;
import de.thm.swtp.api.github.exception.GithubRepoNotLinkedException;
import de.thm.swtp.api.projectGithubRepo.mapper.ProjectGithubRepoMapper;
import de.thm.swtp.api.projectGithubRepo.repository.ProjectGithubRepoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectGithubRepoService {

    private final ProjectGithubRepoRepository projectGithubRepoRepository;
    private final ProjectRepository projectRepository;
    private final GithubConnectionService githubConnectionService;
    private final GithubApiClient githubApiClient;
    private final GithubRepoDataService githubRepoDataService;

    @Transactional
    public GithubRepoCard link(UUID projectId, UUID userId, String repoOwner, String repoName) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        String token = githubConnectionService.getActiveDecryptedToken(userId)
                .orElseThrow(GithubConnectionRequiredException::new);

        GithubApiClient.GithubRepo repo;
        try {
            repo = githubApiClient.getRepository(token, repoOwner, repoName);
        } catch (GithubTokenInvalidException e) {
            githubConnectionService.markInvalid(userId);
            throw e;
        }

        if (repo.isPrivate()) {
            throw new PrivateRepoNotAllowedException();
        }

        ProjectGithubRepoEntity entity = projectGithubRepoRepository.findByProjectId(projectId)
                .orElseGet(() -> ProjectGithubRepoEntity.builder().project(project).build());
        entity.setRepoOwner(repoOwner);
        entity.setRepoName(repoName);
        entity.setLinkedByKeycloakId(userId);

        projectGithubRepoRepository.save(entity);
        return getCard(projectId);
    }

    @Transactional(readOnly = true)
    public GithubRepoCard getCard(UUID projectId) {
        ProjectGithubRepoEntity entity = projectGithubRepoRepository.findByProjectId(projectId)
                .orElseThrow(() -> new GithubRepoNotLinkedException(projectId));

        GithubRepoLink link = ProjectGithubRepoMapper.toDomain(entity);
        var data = githubRepoDataService.fetch(link.getRepoOwner(), link.getRepoName(), link.getLinkedByKeycloakId());

        return GithubRepoCard.builder()
                .link(link)
                .data(data)
                .dataUnavailable(data == null)
                .build();
    }

    @Transactional
    public void unlink(UUID projectId) {
        ProjectGithubRepoEntity entity = projectGithubRepoRepository.findByProjectId(projectId)
                .orElseThrow(() -> new GithubRepoNotLinkedException(projectId));
        projectGithubRepoRepository.delete(entity);
    }
}
