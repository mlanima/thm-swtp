package de.thm.swtp.api.discord.service;

import de.thm.swtp.api.discord.client.DiscordOAuthClient;
import de.thm.swtp.api.discord.client.DiscordOAuthClient.BotTokenResponse;
import de.thm.swtp.api.discord.client.DiscordOAuthClient.BotTokenResponse.Guild;
import de.thm.swtp.api.discord.config.DiscordProperties;
import de.thm.swtp.api.discord.exception.DiscordAccountAlreadyLinkedException;
import de.thm.swtp.api.discord.exception.DiscordConnectionFailedException;
import de.thm.swtp.api.discord.repository.LinkedChannelRepository;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscordAuthServiceTest {

    @Mock
    private DiscordOAuthClient discordOAuthClient;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private LinkedChannelRepository linkedChannelRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private DiscordProperties discordProperties;

    @Mock
    private DiscordProperties.OAuth oauth;

    private DiscordAuthService service;

    private UUID userId;
    private UUID projectId;
    private UserProfile profile;
    private ProjectEntity project;
    private UserProfile owner;

    @BeforeEach
    void setUp() {
        when(discordProperties.getOauth()).thenReturn(oauth);
        when(oauth.getClientId()).thenReturn("test-client-id");
        when(oauth.getRedirectUri()).thenReturn("http://localhost:8080/callback");

        service = new DiscordAuthService(discordOAuthClient, userProfileRepository,
                linkedChannelRepository, projectRepository, discordProperties,
                "http://localhost:4200");

        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        profile = UserProfile.builder()
                .keycloakId(userId)
                .username("User")
                .build();
        owner = UserProfile.builder()
                .keycloakId(userId)
                .username("Owner")
                .discordId("discord-owner-123")
                .build();
        project = ProjectEntity.builder()
                .id(projectId)
                .name("Test")
                .owner(owner)
                .build();
    }

    // ── isOAuthConfigured ──

    @Test
    void shouldBeConfiguredWhenClientIdAndRedirectUriPresent() {
        assertThat(service.isOAuthConfigured()).isTrue();
    }

    @Test
    void shouldNotBeConfiguredWhenClientIdMissing() {
        when(oauth.getClientId()).thenReturn(null);

        service = new DiscordAuthService(discordOAuthClient, userProfileRepository,
                linkedChannelRepository, projectRepository, discordProperties,
                "http://localhost:4200");

        assertThat(service.isOAuthConfigured()).isFalse();
    }

    @Test
    void shouldNotBeConfiguredWhenRedirectUriBlank() {
        when(oauth.getRedirectUri()).thenReturn("");

        service = new DiscordAuthService(discordOAuthClient, userProfileRepository,
                linkedChannelRepository, projectRepository, discordProperties,
                "http://localhost:4200");

        assertThat(service.isOAuthConfigured()).isFalse();
    }

    // ── buildAuthorizationUrl ──

    @Test
    void shouldBuildAuthorizationUrl() {
        var url = service.buildAuthorizationUrl(userId);

        assertThat(url).contains("discord.com/api/oauth2/authorize");
        assertThat(url).contains("client_id=test-client-id");
        assertThat(url).contains("redirect_uri=http://localhost:8080/callback");
        assertThat(url).contains("state=user:");
    }

    // ── createBotAuthUrl ──

    @Test
    void shouldCreateBotAuthUrl() {
        var url = service.createBotAuthUrl(projectId, userId, "76817");

        assertThat(url).contains("discord.com/api/oauth2/authorize");
        assertThat(url).contains("permissions=76817");
        assertThat(url).contains("state=bot:");
    }

    // ── parseNonceFromState ──

    @Test
    void shouldParseNonceFromBotState() {
        var nonce = service.parseNonceFromState("bot:abc-123");

        assertThat(nonce).isEqualTo("abc-123");
    }

    @Test
    void shouldParseNonceFromUserState() {
        var nonce = service.parseNonceFromState("user:xyz-789");

        assertThat(nonce).isEqualTo("xyz-789");
    }

    @Test
    void shouldReturnNullForUnknownState() {
        assertThat(service.parseNonceFromState("other:value")).isNull();
    }

    @Test
    void shouldReturnNullForNullState() {
        assertThat(service.parseNonceFromState(null)).isNull();
    }

    // ── handleBotCallback ──

    @Test
    void shouldHandleBotCallback() {
        var url = service.createBotAuthUrl(projectId, userId, "76817");
        var nonce = service.parseNonceFromState(url.split("state=")[1]);

        var guild = new Guild("g-1", "My Guild", "discord-owner-123");
        var tokenResp = new BotTokenResponse("token", "bot", guild);
        when(discordOAuthClient.exchangeBotCode("code-123")).thenReturn(tokenResp);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        service.handleBotCallback(nonce, "code-123");

        var consumedGuildId = service.consumeBotGuild(projectId);
        assertThat(consumedGuildId).isEqualTo("g-1");
    }

    @Test
    void shouldThrowWhenBotCallbackNonceInvalid() {
        assertThatThrownBy(() -> service.handleBotCallback("invalid-nonce", "code"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nonce");
    }

    @Test
    void shouldThrowWhenBotCallbackProjectNotFound() {
        var url = service.createBotAuthUrl(projectId, userId, "76817");
        var nonce = service.parseNonceFromState(url.split("state=")[1]);

        var guild = new Guild("g-1", "My Guild", "discord-owner-123");
        var tokenResp = new BotTokenResponse("token", "bot", guild);
        when(discordOAuthClient.exchangeBotCode("code")).thenReturn(tokenResp);
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handleBotCallback(nonce, "code"))
                .isInstanceOf(DiscordConnectionFailedException.class);
    }

    @Test
    void shouldThrowWhenBotCallbackOwnerNotLinked() {
        owner.setDiscordId(null);

        var url = service.createBotAuthUrl(projectId, userId, "76817");
        var nonce = service.parseNonceFromState(url.split("state=")[1]);

        var guild = new Guild("g-1", "My Guild", "other-owner");
        var tokenResp = new BotTokenResponse("token", "bot", guild);
        when(discordOAuthClient.exchangeBotCode("code")).thenReturn(tokenResp);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service.handleBotCallback(nonce, "code"))
                .isInstanceOf(DiscordConnectionFailedException.class);
    }

    @Test
    void shouldThrowWhenGuildOwnerMismatch() {
        var url = service.createBotAuthUrl(projectId, userId, "76817");
        var nonce = service.parseNonceFromState(url.split("state=")[1]);

        var guild = new Guild("g-1", "My Guild", "different-owner");
        var tokenResp = new BotTokenResponse("token", "bot", guild);
        when(discordOAuthClient.exchangeBotCode("code")).thenReturn(tokenResp);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service.handleBotCallback(nonce, "code"))
                .isInstanceOf(DiscordConnectionFailedException.class)
                .hasMessageContaining("must be the owner");
    }

    // ── handleBotGuildOnly ──

    @Test
    void shouldHandleBotGuildOnly() {
        var url = service.createBotAuthUrl(projectId, userId, "76817");
        var nonce = service.parseNonceFromState(url.split("state=")[1]);

        service.handleBotGuildOnly(nonce, "guild-1");

        var consumed = service.consumeBotGuild(projectId);
        assertThat(consumed).isEqualTo("guild-1");
    }

    @Test
    void shouldThrowWhenBotGuildOnlyInvalidNonce() {
        assertThatThrownBy(() -> service.handleBotGuildOnly("bad-nonce", "guild-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── storeBotGuild / consumeBotGuild ──

    @Test
    void shouldStoreAndConsumeBotGuild() {
        service.storeBotGuild(projectId, "guild-1");

        assertThat(service.consumeBotGuild(projectId)).isEqualTo("guild-1");
        assertThat(service.consumeBotGuild(projectId)).isNull();
    }

    // ── isPendingBotAuth ──

    @Test
    void shouldDetectPendingBotAuth() {
        service.createBotAuthUrl(projectId, userId, "76817");

        assertThat(service.isPendingBotAuth(projectId)).isTrue();
    }

    @Test
    void shouldNotDetectPendingBotAuthAfterConsume() {
        var url = service.createBotAuthUrl(projectId, userId, "76817");
        var nonce = service.parseNonceFromState(url.split("state=")[1]);

        service.handleBotGuildOnly(nonce, "guild-1");

        assertThat(service.isPendingBotAuth(projectId)).isFalse();
    }

    // ── handleCallback (user linking) ──

    @Test
    void shouldLinkDiscordAccount() {
        var url = service.buildAuthorizationUrl(userId);
        var nonce = service.parseNonceFromState(url.split("state=")[1]);

        when(discordOAuthClient.exchangeCode("auth-code"))
                .thenReturn(Map.of("access_token", "token-123"));
        when(discordOAuthClient.getUser("token-123"))
                .thenReturn(Map.of("id", "discord-id-456", "username", "Danny", "avatar", "abc123"));
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.handleCallback("auth-code", nonce);

        assertThat(result.getDiscordId()).isEqualTo("discord-id-456");
        assertThat(result.getDiscordUsername()).isEqualTo("Danny");
    }

    @Test
    void shouldThrowWhenCallbackStateInvalid() {
        assertThatThrownBy(() -> service.handleCallback("code", "invalid-state"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("state");
    }

    @Test
    void shouldThrowWhenUserAlreadyLinkedToDifferentDiscord() {
        var url = service.buildAuthorizationUrl(userId);
        var nonce = service.parseNonceFromState(url.split("state=")[1]);

        profile.setDiscordId("other-discord-id");

        when(discordOAuthClient.exchangeCode("code"))
                .thenReturn(Map.of("access_token", "token"));
        when(discordOAuthClient.getUser("token"))
                .thenReturn(Map.of("id", "different-id", "username", "Other", "avatar", "def456"));
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.handleCallback("code", nonce))
                .isInstanceOf(DiscordAccountAlreadyLinkedException.class);
    }

    // ── disconnect ──

    @Test
    void shouldDisconnectDiscord() {
        profile.setDiscordId("discord-id");
        profile.setDiscordUsername("User#1234");
        profile.setDiscordAvatar("avatar-hash");
        profile.setDiscordConnectedAt(java.time.LocalDateTime.now());

        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.disconnect(userId);

        assertThat(profile.getDiscordId()).isNull();
        assertThat(profile.getDiscordUsername()).isNull();
        assertThat(profile.getDiscordAvatar()).isNull();
        assertThat(profile.getDiscordConnectedAt()).isNull();
    }

    @Test
    void shouldThrowWhenDisconnectUserNotFound() {
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.disconnect(userId))
                .isInstanceOf(RuntimeException.class);
    }

    // ── purgeStaleBotNonces ──

    @Test
    void shouldPurgeExpiredBotNonces() {
        service.createBotAuthUrl(projectId, userId, "76817");

        assertThat(service.isPendingBotAuth(projectId)).isTrue();
    }

    @Test
    void shouldKeepNonExpiredBotNonces() {
        service.createBotAuthUrl(projectId, userId, "76817");

        service.purgeStaleBotNonces();

        assertThat(service.isPendingBotAuth(projectId)).isTrue();
    }

    // ── getters ──

    @Test
    void shouldReturnFrontendUrl() {
        assertThat(service.getFrontendUrl()).isEqualTo("http://localhost:4200");
    }

    @Test
    void shouldReturnRedirectUri() {
        assertThat(service.getRedirectUri()).isEqualTo("http://localhost:8080/callback");
    }

    @Test
    void shouldHandleUserCallback_avatarGif() {
        var url = service.buildAuthorizationUrl(userId);
        var nonce = service.parseNonceFromState(url.split("state=")[1]);

        when(discordOAuthClient.exchangeCode("code"))
                .thenReturn(Map.of("access_token", "token"));
        when(discordOAuthClient.getUser("token"))
                .thenReturn(Map.of("id", "discord-id", "username", "User", "avatar", "a_animated123"));
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.handleCallback("code", nonce);

        assertThat(result.getDiscordAvatar()).endsWith(".gif");
    }

    @Test
    void shouldHandleUserCallback_noAvatar() {
        var url = service.buildAuthorizationUrl(userId);
        var nonce = service.parseNonceFromState(url.split("state=")[1]);

        when(discordOAuthClient.exchangeCode("code"))
                .thenReturn(Map.of("access_token", "token"));
        when(discordOAuthClient.getUser("token"))
                .thenReturn(Map.of("id", "discord-id", "username", "User"));
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.handleCallback("code", nonce);

        assertThat(result.getDiscordAvatar()).isNull();
    }
}
