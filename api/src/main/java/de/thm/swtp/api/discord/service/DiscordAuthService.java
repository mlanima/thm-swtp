package de.thm.swtp.api.discord.service;

import de.thm.swtp.api.discord.client.DiscordOAuthClient;
import de.thm.swtp.api.discord.exception.DiscordAccountAlreadyLinkedException;
import de.thm.swtp.api.discord.exception.DiscordConnectionFailedException;
import de.thm.swtp.api.discord.repository.LinkedChannelRepository;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static de.thm.swtp.api.discord.service.DiscordAuthService.StatePrefix.BOT_PREFIX;
import static de.thm.swtp.api.discord.service.DiscordAuthService.StatePrefix.USER_PREFIX;

@Service
@Slf4j
public class DiscordAuthService {

    private final DiscordOAuthClient discordOAuthClient;
    private final UserProfileRepository userProfileRepository;
    private final ConcurrentHashMap<String, UUID> pendingStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PendingBotGuild> pendingBotGuilds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BotNonce> pendingBotNonces = new ConcurrentHashMap<>();

    private final String clientId;
    private final String redirectUri;
    private final String frontendUrl;

    private final LinkedChannelRepository linkedChannelRepository;
    private final ProjectRepository projectRepository;

    public DiscordAuthService(DiscordOAuthClient discordOAuthClient,
                              UserProfileRepository userProfileRepository,
                              LinkedChannelRepository linkedChannelRepository,
                              ProjectRepository projectRepository,
                              @Value("${DISCORD_CLIENT_ID:}") String clientId,
                              @Value("${DISCORD_REDIRECT_URI:}") String redirectUri,
                              @Value("${app.frontend-url}") String frontendUrl) {
        this.discordOAuthClient = discordOAuthClient;
        this.userProfileRepository = userProfileRepository;
        this.linkedChannelRepository = linkedChannelRepository;
        this.projectRepository = projectRepository;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.frontendUrl = frontendUrl;
        log.info("Discord OAuth configured: clientId={}, redirectUri={}, frontendUrl={}",
                clientId != null && !clientId.isBlank() ? "present" : "missing",
                redirectUri != null && !redirectUri.isBlank() ? redirectUri : "missing",
                frontendUrl);
    }

    public enum StatePrefix {
        USER_PREFIX("user:"),
        BOT_PREFIX("bot:");

        private final String prefix;

        StatePrefix(String prefix) {
            this.prefix = prefix;
        }

        public String prefix() {
            return prefix;
        }
    }

    public boolean isOAuthConfigured() {
        return clientId != null && !clientId.isBlank() && redirectUri != null && !redirectUri.isBlank();
    }

    public String buildAuthorizationUrl(UUID userId) {
        String nonce = UUID.randomUUID().toString();
        String state = USER_PREFIX.prefix() + nonce;
        pendingStates.put(nonce, userId);
        return "https://discord.com/api/oauth2/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=identify"
                + "&state=" + state;
    }

    public String createBotAuthUrl(UUID projectId, UUID userId, String permissions, String redirectUri) {
        String nonce = UUID.randomUUID().toString();
        String state = BOT_PREFIX.prefix() + nonce;
        pendingBotNonces.put(nonce, new BotNonce(projectId, userId));
        return "https://discord.com/api/oauth2/authorize"
                + "?client_id=" + clientId
                + "&permissions=" + permissions
                + "&scope=bot%20identify"
                + "&response_type=code"
                + "&redirect_uri=" + redirectUri
                + "&state=" + state;
    }

    public BotNonce consumeBotNonce(String nonce) {
        var entry = pendingBotNonces.remove(nonce);
        return entry;
    }

    public String parseNonceFromState(String state) {
        if (state == null) {
            return null;
        }
        if (state.startsWith(BOT_PREFIX.prefix())) {
            return state.substring(BOT_PREFIX.prefix().length());
        }
        if (state.startsWith(USER_PREFIX.prefix())) {
            return state.substring(USER_PREFIX.prefix().length());
        }
        return null;
    }

    private record BotNonce(UUID projectId, UUID userId) {
        BotNonce(UUID projectId, UUID userId) {
            this.projectId = projectId;
            this.userId = userId;
        }
    }

    @Scheduled(fixedRate = 300_000)
    public void purgeStaleBotNonces() {
        pendingBotNonces.clear();
    }

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void storeBotGuild(UUID projectId, String guildId) {
        pendingBotGuilds.put(projectId, new PendingBotGuild(guildId));
        log.info("Bot guild stored for project {}: guildId={}", projectId, guildId);
    }

    public String consumeBotGuild(UUID projectId) {
        var entry = pendingBotGuilds.remove(projectId);
        return entry != null ? entry.guildId() : null;
    }

    @Scheduled(fixedRate = 300_000)
    public void purgeStaleBotGuilds() {
        pendingBotGuilds.values().removeIf(PendingBotGuild::isExpired);
    }

    @Transactional
    public void handleBotCallback(String nonce, String code) {
        BotNonce botNonce = consumeBotNonce(nonce);
        if (botNonce == null) {
            throw new IllegalArgumentException("Invalid or expired bot nonce");
        }

        UUID projectId = botNonce.projectId();

        var tokenResp = discordOAuthClient.exchangeBotCode(code);
        String guildId = tokenResp.guild().id();
        String guildOwnerId = tokenResp.guild().ownerId();

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DiscordConnectionFailedException("Project not found"));

        String ownerDiscordId = project.getOwner().getDiscordId();
        if (ownerDiscordId == null || ownerDiscordId.isBlank()) {
            throw new DiscordConnectionFailedException(
                    "You must link your Discord account in profile settings before connecting a Discord server");
        }

        if (guildOwnerId == null || !guildOwnerId.equals(ownerDiscordId)) {
            log.warn("Guild owner mismatch: guildOwnerId={}, expected project owner discordId={}",
                    guildOwnerId, ownerDiscordId);
            throw new DiscordConnectionFailedException(
                    "You must be the owner of the Discord server to bind it");
        }

        var linksWithGuild = linkedChannelRepository.findAllByDiscordGuildIdAndIsActiveTrue(guildId);
        for (var link : linksWithGuild) {
            if (!link.getProject().getId().equals(projectId)) {
                throw new DiscordConnectionFailedException(
                        "This Discord server is already linked to another project");
            }
        }

        storeBotGuild(projectId, guildId);
        log.info("Bot guild captured via token exchange: project={}, guildId={}, guildName={}",
                projectId, guildId, tokenResp.guild().name());
    }

    private record PendingBotGuild(String guildId, Instant createdAt) {
        PendingBotGuild(String guildId) {
            this(guildId, Instant.now());
        }

        boolean isExpired() {
            return Duration.between(createdAt, Instant.now()).toMinutes() >= 30;
        }
    }

    private String buildAvatarUrl(String discordId, Object avatarField) {
        if (avatarField instanceof String hash && !hash.isBlank()) {
            String extension = hash.startsWith("a_") ? "gif" : "png";
            return "https://cdn.discordapp.com/avatars/" + discordId + "/" + hash + "." + extension;
        }
        return null;
    }

    @Transactional
    public UserProfile handleCallback(String code, String state) {
        UUID userId = pendingStates.remove(state);
        if (userId == null) {
            throw new IllegalArgumentException("Invalid or expired state parameter");
        }

        Map<String, Object> tokenResponse = discordOAuthClient.exchangeCode(code);
        String accessToken = (String) tokenResponse.get("access_token");

        Map<String, Object> discordUser = discordOAuthClient.getUser(accessToken);
        String discordId = discordUser.get("id").toString();
        String discordUsername = (String) discordUser.get("username");
        Object avatarField = discordUser.get("avatar");
        String discordAvatar = buildAvatarUrl(discordId, avatarField);

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (profile.getDiscordId() != null && !profile.getDiscordId().equals(discordId)) {
            throw new DiscordAccountAlreadyLinkedException(
                    "Discord account already linked to another platform account");
        }

        userProfileRepository.findByDiscordId(discordId)
                .filter(existing -> !existing.getKeycloakId().equals(userId))
                .ifPresent(existing -> {
                    throw new DiscordAccountAlreadyLinkedException(
                            "This Discord account is already linked to another user");
                });

        profile.setDiscordId(discordId);
        profile.setDiscordUsername(discordUsername);
        profile.setDiscordAvatar(discordAvatar);
        profile.setDiscordConnectedAt(LocalDateTime.now());
        UserProfile saved = userProfileRepository.save(profile);
        log.info("Discord account linked: user={}, discordId={}", userId, discordId);
        return saved;
    }

    @Transactional
    public void disconnect(UUID userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        profile.setDiscordId(null);
        profile.setDiscordUsername(null);
        profile.setDiscordAvatar(null);
        profile.setDiscordConnectedAt(null);
        userProfileRepository.save(profile);
        log.info("Discord account disconnected: user={}", userId);
    }
}
