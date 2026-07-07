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

        if (guildId != null && state != null) {
            return handleBotCallback(UUID.fromString(state), guildId);
        }

        if (error != null) {
            log.warn("Discord OAuth error: {}", error);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(discordAuthService.getFrontendUrl() + "/settings?discord=error"))
                    .build();
        }

        return handleUserCallback(code, state);
    }

    private ResponseEntity<?> handleBotCallback(UUID projectId, String guildId) {
        discordAuthService.storeBotGuild(projectId, guildId);
        String html = "<!DOCTYPE html><html><body><script>window.close()</script></body></html>";
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    private ResponseEntity<Void> handleUserCallback(String code, String state) {
        try {
            UserProfile profile = discordAuthService.handleCallback(code, state);
            log.info("Discord account linked successfully: discordId={}", profile.getDiscordId());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(discordAuthService.getFrontendUrl() + "/settings?discord=connected"))
                    .build();
        } catch (Exception e) {
            log.warn("Discord callback failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(discordAuthService.getFrontendUrl() + "/settings?discord=error"))
                    .build();
        }
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        discordAuthService.disconnect(userId);
        return ResponseEntity.noContent().build();
    }
}
