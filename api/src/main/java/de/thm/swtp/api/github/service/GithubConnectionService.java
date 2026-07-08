package de.thm.swtp.api.github.service;

import de.thm.swtp.api.github.client.GithubApiClient;
import de.thm.swtp.api.github.client.GithubOAuthClient;
import de.thm.swtp.api.github.config.GithubOAuthProperties;
import de.thm.swtp.api.github.domain.GithubConnection;
import de.thm.swtp.api.github.domain.GithubConnectionStatus;
import de.thm.swtp.api.exceptionhandling.exceptions.UserProfileLinkAlreadyExistsException;
import de.thm.swtp.api.github.entity.GithubConnectionEntity;
import de.thm.swtp.api.github.exception.GithubIntegrationDisabledException;
import de.thm.swtp.api.github.mapper.GithubConnectionMapper;
import de.thm.swtp.api.github.repository.GithubConnectionRepository;
import de.thm.swtp.api.links.service.UserProfileLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;
import java.util.UUID;

/** Manages the per-user GitHub OAuth connection: authorize-URL generation, code exchange,
 * and disconnect. Other features (e.g. project-repo linking) go through
 * {@link #getActiveDecryptedToken(UUID)} rather than touching the repository directly. */
@Slf4j
@Service
@RequiredArgsConstructor
public class GithubConnectionService {

    private final GithubConnectionRepository githubConnectionRepository;
    private final GithubOAuthClient githubOAuthClient;
    private final GithubApiClient githubApiClient;
    private final GithubStateService githubStateService;
    private final TokenCipher tokenCipher;
    private final GithubOAuthProperties properties;
    private final UserProfileLinkService userProfileLinkService;

    public String buildAuthorizeUrl(UUID userId) {
        requireEnabled();
        String state = githubStateService.create(userId);

        return UriComponentsBuilder.fromUriString(properties.authorizeUrl())
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("scope", properties.scopes())
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    @Transactional
    public GithubConnection completeConnection(UUID userId, String code, String state) {
        requireEnabled();
        githubStateService.validate(state, userId);

        var accessTokenResult = githubOAuthClient.exchangeCode(code);
        var githubUser = githubApiClient.getAuthenticatedUser(accessTokenResult.accessToken());

        GithubConnectionEntity entity = githubConnectionRepository.findById(userId)
                .orElseGet(() -> GithubConnectionEntity.builder().keycloakId(userId).build());
        entity.setGithubUserId(githubUser.id());
        entity.setGithubLogin(githubUser.login());
        entity.setAvatarUrl(githubUser.avatarUrl());
        entity.setEncryptedAccessToken(tokenCipher.encrypt(accessTokenResult.accessToken()));
        entity.setScopes(accessTokenResult.scope());
        entity.setStatus(GithubConnectionStatus.ACTIVE);

        GithubConnectionEntity saved = githubConnectionRepository.save(entity);
        ensureGithubProfileLink(userId, githubUser.login());
        log.info("GitHub connection established for user {}", userId);
        return GithubConnectionMapper.toDomain(saved);
    }

    @Transactional(readOnly = true)
    public Optional<GithubConnection> getConnection(UUID userId) {
        return githubConnectionRepository.findById(userId).map(GithubConnectionMapper::toDomain);
    }

    @Transactional
    public void disconnect(UUID userId) {
        githubConnectionRepository.findById(userId).ifPresent(entity -> {
            githubOAuthClient.revokeGrant(tokenCipher.decrypt(entity.getEncryptedAccessToken()));
            githubConnectionRepository.delete(entity);
            log.info("GitHub connection removed for user {}", userId);
        });
    }

    @Transactional(readOnly = true)
    public Optional<String> getActiveDecryptedToken(UUID userId) {
        return githubConnectionRepository.findById(userId)
                .filter(entity -> entity.getStatus() == GithubConnectionStatus.ACTIVE)
                .map(entity -> tokenCipher.decrypt(entity.getEncryptedAccessToken()));
    }

    @Transactional
    public void markInvalid(UUID userId) {
        githubConnectionRepository.findById(userId).ifPresent(entity -> {
            entity.setStatus(GithubConnectionStatus.INVALID);
            githubConnectionRepository.save(entity);
            log.warn("GitHub connection marked invalid for user {} (token rejected by GitHub)", userId);
        });
    }

    /** Adds a link to the user's GitHub profile to their user-profile links, unless one
     * already exists (e.g. reconnecting the same account) — a one-time convenience, not a
     * synced field, so it's left alone afterwards even if the user edits or removes it. */
    private void ensureGithubProfileLink(UUID userId, String githubLogin) {
        try {
            userProfileLinkService.createUserProfileLink(userId, "GitHub", "https://github.com/" + githubLogin);
        } catch (UserProfileLinkAlreadyExistsException e) {
            log.debug("GitHub profile link already exists for user {}", userId);
        }
    }

    private void requireEnabled() {
        if (!properties.enabled()) {
            throw new GithubIntegrationDisabledException();
        }
    }
}
