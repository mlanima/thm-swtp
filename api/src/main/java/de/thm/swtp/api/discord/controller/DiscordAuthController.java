package de.thm.swtp.api.discord.controller;

import de.thm.swtp.api.discord.service.DiscordAuthService;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<Map<String, String>> authorize(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String url = discordAuthService.buildAuthorizationUrl(userId);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam String code,
            @RequestParam String state) {
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
