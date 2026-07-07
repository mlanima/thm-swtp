package de.thm.swtp.api.projectGithubRepo.listener;

import de.thm.swtp.api.github.client.GithubApiClient;
import de.thm.swtp.api.github.exception.GithubApiException;
import de.thm.swtp.api.github.exception.GithubTokenInvalidException;
import de.thm.swtp.api.github.service.GithubConnectionService;
import de.thm.swtp.api.notification.event.ProjectMemberAddedEvent;
import de.thm.swtp.api.projectGithubRepo.repository.ProjectGithubRepoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Best-effort: when a project has a linked GitHub repo and has opted in to auto-inviting
 * collaborators, invites new members as GitHub collaborators using the repo-linker's token.
 * Never blocks or affects the membership grant itself — any failure here (no GitHub
 * connection, stale token, GitHub API error) is only logged. */
@Component
@RequiredArgsConstructor
@Slf4j
public class GithubCollaboratorInviteListener {

    private static final String COLLABORATOR_PERMISSION = "push";

    private final ProjectGithubRepoRepository projectGithubRepoRepository;
    private final GithubConnectionService githubConnectionService;
    private final GithubApiClient githubApiClient;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProjectMemberAdded(ProjectMemberAddedEvent event) {
        var repoLinkOpt = projectGithubRepoRepository.findByProjectId(event.projectId());
        if (repoLinkOpt.isEmpty() || !repoLinkOpt.get().isAutoInviteCollaborators()) {
            return;
        }
        var repoLink = repoLinkOpt.get();

        var connectionOpt = githubConnectionService.getConnection(event.newMemberKeycloakId());
        if (connectionOpt.isEmpty()) {
            log.debug("Skipping GitHub collaborator invite for project {}: user {} has no GitHub connection",
                    event.projectId(), event.newMemberKeycloakId());
            return;
        }

        var tokenOpt = githubConnectionService.getActiveDecryptedToken(repoLink.getLinkedByKeycloakId());
        if (tokenOpt.isEmpty()) {
            log.debug("Skipping GitHub collaborator invite for project {}: repo linker has no active connection",
                    event.projectId());
            return;
        }

        String githubLogin = connectionOpt.get().getGithubLogin();
        try {
            githubApiClient.addCollaborator(tokenOpt.get(), repoLink.getRepoOwner(), repoLink.getRepoName(),
                    githubLogin, COLLABORATOR_PERMISSION);
            log.info("Invited GitHub user {} as a collaborator on {}/{}",
                    githubLogin, repoLink.getRepoOwner(), repoLink.getRepoName());
        } catch (GithubTokenInvalidException e) {
            githubConnectionService.markInvalid(repoLink.getLinkedByKeycloakId());
            log.warn("GitHub collaborator invite failed for project {}: repo linker's token was rejected",
                    event.projectId());
        } catch (GithubApiException e) {
            log.warn("GitHub collaborator invite failed for project {}: {}", event.projectId(), e.getMessage());
        }
    }
}
