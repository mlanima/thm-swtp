package de.thm.swtp.api.discord.controller;

import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import de.thm.swtp.api.discord.service.DiscordChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/discord")
@RequiredArgsConstructor
public class DiscordChannelController {

    private final DiscordChannelService discordChannelService;

    @PostMapping("/connect")
    public ResponseEntity<Map<String, Object>> connect(
            @PathVariable UUID projectId,
            @RequestBody Map<String, String> body) {
        String channelId = body.get("channelId");
        if (channelId == null || channelId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "channelId is required"));
        }
        LinkedChannelEntity link = discordChannelService.connectChannel(projectId, channelId);
        Map<String, Object> result = new HashMap<>();
        result.put("id", link.getId());
        result.put("discordChannelId", link.getDiscordChannelId());
        result.put("isActive", link.isActive());
        result.put("discordInviteUrl", link.getDiscordInviteUrl());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/bot-invite")
    public ResponseEntity<Map<String, String>> getBotInviteUrl(@PathVariable UUID projectId) {
        return ResponseEntity.ok(Map.of("url", discordChannelService.getBotInviteUrl()));
    }

    @PostMapping("/auto-connect")
    public ResponseEntity<Map<String, Object>> autoConnect(@PathVariable UUID projectId) {
        LinkedChannelEntity link = discordChannelService.autoConnectChannel(projectId);
        Map<String, Object> result = new HashMap<>();
        result.put("id", link.getId());
        result.put("discordChannelId", link.getDiscordChannelId());
        result.put("isActive", link.isActive());
        result.put("discordInviteUrl", link.getDiscordInviteUrl());
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/invite")
    public ResponseEntity<Map<String, Object>> updateInviteUrl(
            @PathVariable UUID projectId,
            @RequestBody Map<String, String> body) {
        String inviteUrl = body.get("discordInviteUrl");
        if (inviteUrl == null || inviteUrl.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "discordInviteUrl is required"));
        }
        LinkedChannelEntity link = discordChannelService.updateDiscordInviteUrl(projectId, inviteUrl);
        return ResponseEntity.ok(Map.of(
                "discordInviteUrl", link.getDiscordInviteUrl()
        ));
    }

    @DeleteMapping("/connect")
    public ResponseEntity<Void> disconnect(@PathVariable UUID projectId) {
        discordChannelService.disconnectChannel(projectId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/connect")
    public ResponseEntity<Map<String, Object>> getConnection(@PathVariable UUID projectId) {
        LinkedChannelEntity link = discordChannelService.getLinkedChannel(projectId);
        Map<String, Object> result = new HashMap<>();
        result.put("id", link.getId());
        result.put("discordChannelId", link.getDiscordChannelId());
        result.put("isActive", link.isActive());
        result.put("discordInviteUrl", link.getDiscordInviteUrl());
        return ResponseEntity.ok(result);
    }
}
