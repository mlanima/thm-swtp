package de.thm.swtp.api.discord.controller;

import de.thm.swtp.api.discord.service.DiscordAuthService;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/discord")
@RequiredArgsConstructor
@Slf4j
public class DiscordAuthController {

    private final DiscordAuthService discordAuthService;

    @GetMapping("/authorize")
    public ResponseEntity<?> authorize(@AuthenticationPrincipal Jwt jwt) {
        if (!discordAuthService.isOAuthConfigured()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Discord OAuth is not configured"));
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        String url = discordAuthService.buildAuthorizationUrl(userId);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String guildId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {

        String nonce = discordAuthService.parseNonceFromState(state);

        if (state != null && state.startsWith("bot:")) {
            return handleBotCallback(nonce, code, guildId, error);
        }

        if (state != null && state.startsWith("user:")) {
            return handleUserCallback(nonce, code, error);
        }

        if (guildId != null && state != null) {
            return handleLegacyBotCallback(state, guildId);
        }

        log.warn("Discord callback with unrecognised state: state={}", state);
        return redirectToSettings("error");
    }

    private ResponseEntity<?> handleBotCallback(String nonce, String code, String guildId, String error) {
        if (error != null) {
            log.warn("Discord bot authorization denied: {}", error);
            return closePopupResponse();
        }
        if (nonce == null) {
            log.warn("Bot callback missing nonce");
            return closePopupResponse();
        }
        if (code != null) {
            return handleBotTokenExchange(nonce, code);
        }
        if (guildId != null) {
            return handleBotGuildOnly(nonce, guildId);
        }
        log.warn("Bot callback missing code and guildId");
        return closePopupResponse();
    }

    private ResponseEntity<?> handleUserCallback(String nonce, String code, String error) {
        if (error != null) {
            log.warn("Discord OAuth error: {}", error);
            return redirectToSettings("error");
        }
        if (code == null || nonce == null) {
            return redirectToSettings("error");
        }
        return handleUserTokenExchange(nonce, code);
    }

    private ResponseEntity<?> handleLegacyBotCallback(String state, String guildId) {
        try {
            UUID projectId = UUID.fromString(state);
            if (!discordAuthService.isPendingBotAuth(projectId)) {
                log.warn("Legacy bot callback with no pending auth: state={}", state);
                return closePopupResponse();
            }
            discordAuthService.storeBotGuild(projectId, guildId);
            log.info("Bot guild stored via legacy redirect: guildId={}", guildId);
        } catch (IllegalArgumentException e) {
            log.warn("Legacy bot callback with invalid state UUID: {}", state);
        } catch (Exception e) {
            log.warn("Legacy bot callback failed: {}", e.getMessage());
        }
        return closePopupResponse();
    }

    private ResponseEntity<?> handleBotTokenExchange(String nonce, String code) {
        try {
            discordAuthService.handleBotCallback(nonce, code);
            log.info("Bot guild captured via token exchange");
        } catch (Exception e) {
            log.warn("Bot token exchange callback failed: {}", e.getMessage());
        }
        return closePopupResponse();
    }

    private ResponseEntity<?> handleBotGuildOnly(String nonce, String guildId) {
        try {
            discordAuthService.handleBotGuildOnly(nonce, guildId);
            log.info("Bot guild stored via guild-only callback: guildId={}", guildId);
        } catch (Exception e) {
            log.warn("Bot guild-only callback failed: {}", e.getMessage());
        }
        return closePopupResponse();
    }

    private ResponseEntity<?> handleUserTokenExchange(String nonce, String code) {
        try {
            UserProfile profile = discordAuthService.handleCallback(code, nonce);
            log.info("Discord account linked successfully: discordId={}", profile.getDiscordId());
            return redirectToSettings("connected");
        } catch (Exception e) {
            log.warn("Discord user callback failed: {}", e.getMessage());
            return redirectToSettings("error");
        }
    }

    private ResponseEntity<?> redirectToSettings(String status) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(discordAuthService.getFrontendUrl() + "/settings?discord=" + status))
                .build();
    }

    private ResponseEntity<?> closePopupResponse() {
        String html = "<!DOCTYPE html><html><body><script>window.close()</script></body></html>";
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        discordAuthService.disconnect(userId);
        return ResponseEntity.noContent().build();
    }
}
