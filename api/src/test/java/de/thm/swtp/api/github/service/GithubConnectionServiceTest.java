package de.thm.swtp.api.github.service;

import de.thm.swtp.api.github.client.GithubApiClient;
import de.thm.swtp.api.github.client.GithubOAuthClient;
import de.thm.swtp.api.github.config.GithubOAuthProperties;
import de.thm.swtp.api.github.domain.GithubConnectionStatus;
import de.thm.swtp.api.github.entity.GithubConnectionEntity;
import de.thm.swtp.api.github.exception.GithubIntegrationDisabledException;
import de.thm.swtp.api.github.exception.GithubOAuthException;
import de.thm.swtp.api.github.exception.InvalidGithubStateException;
import de.thm.swtp.api.github.repository.GithubConnectionRepository;
import de.thm.swtp.api.exceptionhandling.exceptions.UserProfileLinkAlreadyExistsException;
import de.thm.swtp.api.links.service.UserProfileLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GithubConnectionServiceTest {

    private GithubConnectionRepository githubConnectionRepository;
    private GithubOAuthClient githubOAuthClient;
    private GithubApiClient githubApiClient;
    private GithubStateService githubStateService;
    private TokenCipher tokenCipher;
    private GithubOAuthProperties enabledProperties;
    private UserProfileLinkService userProfileLinkService;

    private GithubConnectionService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        githubConnectionRepository = mock(GithubConnectionRepository.class);
        githubOAuthClient = mock(GithubOAuthClient.class);
        githubApiClient = mock(GithubApiClient.class);
        githubStateService = mock(GithubStateService.class);
        tokenCipher = mock(TokenCipher.class);
        userProfileLinkService = mock(UserProfileLinkService.class);
        enabledProperties = new GithubOAuthProperties(
                "client-id", "client-secret", "http://localhost:4200/github/callback",
                "read:user", "https://github.com/login/oauth/authorize",
                "https://github.com/login/oauth/access_token", "test-key");

        userId = UUID.randomUUID();
        service = new GithubConnectionService(
                githubConnectionRepository, githubOAuthClient, githubApiClient,
                githubStateService, tokenCipher, enabledProperties, userProfileLinkService);
    }

    @Test
    void shouldBuildAuthorizeUrlWhenEnabled() {
        when(githubStateService.create(userId)).thenReturn("signed-state");

        String url = service.buildAuthorizeUrl(userId);

        assertThat(url).startsWith("https://github.com/login/oauth/authorize");
        assertThat(url).contains("client_id=client-id");
        assertThat(url).contains("state=signed-state");
    }

    @Test
    void shouldThrowWhenDisabledOnAuthorizeUrl() {
        var disabledService = new GithubConnectionService(
                githubConnectionRepository, githubOAuthClient, githubApiClient,
                githubStateService, tokenCipher,
                new GithubOAuthProperties("", "", "redirect", "read:user", "authorize", "token", ""),
                userProfileLinkService);

        assertThatThrownBy(() -> disabledService.buildAuthorizeUrl(userId))
                .isInstanceOf(GithubIntegrationDisabledException.class);
    }

    @Test
    void shouldCompleteConnectionOnHappyPath() {
        when(githubOAuthClient.exchangeCode("valid-code"))
                .thenReturn(new GithubOAuthClient.AccessTokenResult("gho_token", "read:user", "bearer", null, null));
        when(githubApiClient.getAuthenticatedUser("gho_token"))
                .thenReturn(new GithubApiClient.GithubUser(1L, "octocat", "The Octocat", "https://avatars.example/1"));
        when(tokenCipher.encrypt("gho_token")).thenReturn("encrypted-token");
        when(githubConnectionRepository.findById(userId)).thenReturn(Optional.empty());
        when(githubConnectionRepository.save(any(GithubConnectionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var connection = service.completeConnection(userId, "valid-code", "any-state");

        assertThat(connection.getGithubLogin()).isEqualTo("octocat");
        assertThat(connection.getStatus()).isEqualTo(GithubConnectionStatus.ACTIVE);
        verify(githubStateService).validate("any-state", userId);
        verify(githubConnectionRepository).save(any(GithubConnectionEntity.class));
        verify(userProfileLinkService).createUserProfileLink(userId, "GitHub", "https://github.com/octocat");
    }

    @Test
    void shouldIgnoreAlreadyExistingProfileLinkOnCompleteConnection() {
        when(githubOAuthClient.exchangeCode("valid-code"))
                .thenReturn(new GithubOAuthClient.AccessTokenResult("gho_token", "read:user", "bearer", null, null));
        when(githubApiClient.getAuthenticatedUser("gho_token"))
                .thenReturn(new GithubApiClient.GithubUser(1L, "octocat", "The Octocat", "https://avatars.example/1"));
        when(tokenCipher.encrypt("gho_token")).thenReturn("encrypted-token");
        when(githubConnectionRepository.findById(userId)).thenReturn(Optional.empty());
        when(githubConnectionRepository.save(any(GithubConnectionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new UserProfileLinkAlreadyExistsException("The link already exists for this user profile."))
                .when(userProfileLinkService).createUserProfileLink(userId, "GitHub", "https://github.com/octocat");

        var connection = service.completeConnection(userId, "valid-code", "any-state");

        assertThat(connection.getGithubLogin()).isEqualTo("octocat");
    }

    @Test
    void shouldThrowWhenDisabledOnCompleteConnection() {
        var disabledService = new GithubConnectionService(
                githubConnectionRepository, githubOAuthClient, githubApiClient,
                githubStateService, tokenCipher,
                new GithubOAuthProperties("", "", "redirect", "read:user", "authorize", "token", ""),
                userProfileLinkService);

        assertThatThrownBy(() -> disabledService.completeConnection(userId, "code", "state"))
                .isInstanceOf(GithubIntegrationDisabledException.class);
        verifyNoInteractions(githubOAuthClient, githubApiClient);
    }

    @Test
    void shouldPropagateInvalidState() {
        doThrow(new InvalidGithubStateException("bad state"))
                .when(githubStateService).validate("bad-state", userId);

        assertThatThrownBy(() -> service.completeConnection(userId, "code", "bad-state"))
                .isInstanceOf(InvalidGithubStateException.class);
        verifyNoInteractions(githubOAuthClient);
    }

    @Test
    void shouldPropagateOAuthErrorFromCodeExchange() {
        when(githubOAuthClient.exchangeCode("bad-code"))
                .thenThrow(new GithubOAuthException("GitHub rejected the authorization code"));

        assertThatThrownBy(() -> service.completeConnection(userId, "bad-code", "any-state"))
                .isInstanceOf(GithubOAuthException.class);
        verifyNoInteractions(githubApiClient);
    }

    @Test
    void shouldRevokeGrantAndDeleteOnDisconnect() {
        var entity = GithubConnectionEntity.builder()
                .keycloakId(userId)
                .githubUserId(1L)
                .githubLogin("octocat")
                .encryptedAccessToken("encrypted-token")
                .status(GithubConnectionStatus.ACTIVE)
                .build();
        when(githubConnectionRepository.findById(userId)).thenReturn(Optional.of(entity));
        when(tokenCipher.decrypt("encrypted-token")).thenReturn("gho_token");

        service.disconnect(userId);

        verify(githubOAuthClient).revokeGrant("gho_token");
        verify(githubConnectionRepository).delete(entity);
    }

    @Test
    void shouldNoOpOnDisconnectWhenNoConnectionExists() {
        when(githubConnectionRepository.findById(userId)).thenReturn(Optional.empty());

        service.disconnect(userId);

        verifyNoInteractions(githubOAuthClient);
        verify(githubConnectionRepository, never()).delete(any());
    }

    @Test
    void shouldReturnActiveTokenOnlyWhenStatusActive() {
        var invalidEntity = GithubConnectionEntity.builder()
                .keycloakId(userId)
                .encryptedAccessToken("encrypted-token")
                .status(GithubConnectionStatus.INVALID)
                .build();
        when(githubConnectionRepository.findById(userId)).thenReturn(Optional.of(invalidEntity));

        assertThat(service.getActiveDecryptedToken(userId)).isEmpty();
        verifyNoInteractions(tokenCipher);
    }

    @Test
    void shouldMarkConnectionInvalid() {
        var entity = GithubConnectionEntity.builder()
                .keycloakId(userId)
                .status(GithubConnectionStatus.ACTIVE)
                .build();
        when(githubConnectionRepository.findById(userId)).thenReturn(Optional.of(entity));

        service.markInvalid(userId);

        assertThat(entity.getStatus()).isEqualTo(GithubConnectionStatus.INVALID);
        verify(githubConnectionRepository).save(entity);
    }
}
