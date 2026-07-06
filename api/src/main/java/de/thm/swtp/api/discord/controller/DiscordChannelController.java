package de.thm.swtp.api.discord.controller;

import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import de.thm.swtp.api.discord.service.DiscordChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        return ResponseEntity.ok(Map.of(
                "id", link.getId(),
                "discordChannelId", link.getDiscordChannelId(),
                "isActive", link.isActive()
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
        return ResponseEntity.ok(Map.of(
                "id", link.getId(),
                "discordChannelId", link.getDiscordChannelId(),
                "isActive", link.isActive()
        ));
    }
}
