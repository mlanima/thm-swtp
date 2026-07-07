package de.thm.swtp.api.discord.service;

import de.thm.swtp.api.discord.client.DiscordOAuthClient;
import de.thm.swtp.api.discord.exception.DiscordAccountAlreadyLinkedException;
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

@Service
@Slf4j
public class DiscordAuthService {

    private final DiscordOAuthClient discordOAuthClient;
    private final UserProfileRepository userProfileRepository;
    private final ConcurrentHashMap<String, UUID> pendingStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PendingBotGuild> pendingBotGuilds = new ConcurrentHashMap<>();

    private final String clientId;
    private final String redirectUri;
    private final String frontendUrl;

    public DiscordAuthService(DiscordOAuthClient discordOAuthClient,
                              UserProfileRepository userProfileRepository,
                              @Value("${DISCORD_CLIENT_ID:}") String clientId,
                              @Value("${DISCORD_REDIRECT_URI:}") String redirectUri,
                              @Value("${app.frontend-url}") String frontendUrl) {
        this.discordOAuthClient = discordOAuthClient;
        this.userProfileRepository = userProfileRepository;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.frontendUrl = frontendUrl;
        log.info("Discord OAuth configured: clientId={}, redirectUri={}, frontendUrl={}",
                clientId != null && !clientId.isBlank() ? "present" : "missing",
                redirectUri != null && !redirectUri.isBlank() ? redirectUri : "missing",
                frontendUrl);
    }

    public boolean isOAuthConfigured() {
        return clientId != null && !clientId.isBlank() && redirectUri != null && !redirectUri.isBlank();
    }

    public String buildAuthorizationUrl(UUID userId) {
        String state = UUID.randomUUID().toString();
        pendingStates.put(state, userId);
        return "https://discord.com/api/oauth2/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=identify"
                + "&state=" + state;
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
