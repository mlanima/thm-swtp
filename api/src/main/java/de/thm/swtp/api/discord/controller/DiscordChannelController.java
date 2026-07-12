package de.thm.swtp.api.discord.controller;

import de.thm.swtp.api.discord.client.BotOperations.GuildInfo;
import de.thm.swtp.api.discord.dto.DiscordChannelResponse;
import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import de.thm.swtp.api.discord.service.DiscordChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the Discord channel connection for a project.
 * Supports linking, auto-connecting, and updating invite URLs.
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/discord")
@RequiredArgsConstructor
public class DiscordChannelController {

    private final DiscordChannelService discordChannelService;

    /** Links a Discord channel (and optionally a guild) to a project. */
    @PostMapping("/connect")
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public ResponseEntity<DiscordChannelResponse> connect(
            @PathVariable UUID projectId,
            @RequestBody Map<String, String> body) {
        String channelId = body.get("channelId");
        String guildId = body.get("guildId");
        if (channelId == null || channelId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        LinkedChannelEntity link = discordChannelService.connectChannel(projectId, channelId, guildId);
        return ResponseEntity.ok(DiscordChannelResponse.from(link));
    }

    /** Returns the Discord bot invite URL so the user can add the bot to their server. */
    @GetMapping("/bot-invite")
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public ResponseEntity<Map<String, String>> getBotInviteUrl(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(Map.of("url", discordChannelService.getBotInviteUrl(projectId, userId)));
    }

    /** Lists Discord servers (guilds) the bot has access to for this project. */
    @GetMapping("/guilds")
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public ResponseEntity<List<GuildInfo>> getGuilds(@PathVariable UUID projectId) {
        return ResponseEntity.ok(discordChannelService.getAvailableGuilds(projectId));
    }

    /** Picks a channel automatically from the given or first available guild. */
    @PostMapping("/auto-connect")
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public ResponseEntity<DiscordChannelResponse> autoConnect(
            @PathVariable UUID projectId,
            @RequestBody(required = false) Map<String, String> body) {
        String guildId = body != null ? body.get("guildId") : null;
        return ResponseEntity.ok(discordChannelService.autoConnectChannel(projectId, guildId));
    }

    /** Updates the vanity invite URL shown on the project page. */
    @PatchMapping("/invite")
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public ResponseEntity<DiscordChannelResponse> updateInviteUrl(
            @PathVariable UUID projectId,
            @RequestBody Map<String, String> body) {
        String inviteUrl = body.get("discordInviteUrl");
        if (inviteUrl == null || inviteUrl.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        LinkedChannelEntity link = discordChannelService.updateDiscordInviteUrl(projectId, inviteUrl);
        return ResponseEntity.ok(DiscordChannelResponse.from(link));
    }

    /** Unlinks the Discord channel from the project. */
    @DeleteMapping("/connect")
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public ResponseEntity<Void> disconnect(@PathVariable UUID projectId) {
        discordChannelService.disconnectChannel(projectId);
        return ResponseEntity.noContent().build();
    }

    /** Returns the currently linked channel for this project, if any. */
    @GetMapping("/connect")
    @PreAuthorize("@security.canViewProject(#projectId, authentication)")
    public ResponseEntity<DiscordChannelResponse> getConnection(@PathVariable UUID projectId) {
        LinkedChannelEntity link = discordChannelService.getLinkedChannel(projectId);
        return ResponseEntity.ok(DiscordChannelResponse.from(link));
    }
}
