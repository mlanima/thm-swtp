package de.thm.swtp.api.discord.controller;

import de.thm.swtp.api.discord.client.BotInternalClient.GuildInfo;
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

@RestController
@RequestMapping("/api/v1/projects/{projectId}/discord")
@RequiredArgsConstructor
public class DiscordChannelController {

    private final DiscordChannelService discordChannelService;

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

    @GetMapping("/bot-invite")
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public ResponseEntity<Map<String, String>> getBotInviteUrl(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(Map.of("url", discordChannelService.getBotInviteUrl(projectId, userId)));
    }

    @GetMapping("/guilds")
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public ResponseEntity<List<GuildInfo>> getGuilds(@PathVariable UUID projectId) {
        return ResponseEntity.ok(discordChannelService.getAvailableGuilds(projectId));
    }

    @PostMapping("/auto-connect")
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public ResponseEntity<DiscordChannelResponse> autoConnect(
            @PathVariable UUID projectId,
            @RequestBody(required = false) Map<String, String> body) {
        String guildId = body != null ? body.get("guildId") : null;
        LinkedChannelEntity link = discordChannelService.autoConnectChannel(projectId, guildId);
        return ResponseEntity.ok(DiscordChannelResponse.from(link));
    }

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

    @DeleteMapping("/connect")
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public ResponseEntity<Void> disconnect(@PathVariable UUID projectId) {
        discordChannelService.disconnectChannel(projectId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/connect")
    @PreAuthorize("@security.canViewProject(#projectId, authentication)")
    public ResponseEntity<DiscordChannelResponse> getConnection(@PathVariable UUID projectId) {
        LinkedChannelEntity link = discordChannelService.getLinkedChannel(projectId);
        return ResponseEntity.ok(DiscordChannelResponse.from(link));
    }
}
