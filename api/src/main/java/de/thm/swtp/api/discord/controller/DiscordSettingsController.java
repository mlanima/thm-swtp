package de.thm.swtp.api.discord.controller;

import de.thm.swtp.api.discord.entity.DiscordChannelSettingsEntity;
import de.thm.swtp.api.discord.service.DiscordChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/discord/settings")
@RequiredArgsConstructor
public class DiscordSettingsController {

    private final DiscordChannelService discordChannelService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettings(@PathVariable UUID projectId) {
        DiscordChannelSettingsEntity settings = discordChannelService.getSettings(projectId);
        return ResponseEntity.ok(Map.of(
                "notifyPostCreated", settings.isNotifyPostCreated(),
                "notifyPostUpdated", settings.isNotifyPostUpdated(),
                "notifyPostDeleted", settings.isNotifyPostDeleted(),
                "notifyMemberJoin", settings.isNotifyMemberJoin(),
                "notifyMemberLeave", settings.isNotifyMemberLeave()
        ));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateSettings(
            @PathVariable UUID projectId,
            @RequestBody Map<String, Boolean> body) {
        DiscordChannelSettingsEntity updated = new DiscordChannelSettingsEntity();
        updated.setNotifyPostCreated(body.getOrDefault("notifyPostCreated", true));
        updated.setNotifyPostUpdated(body.getOrDefault("notifyPostUpdated", true));
        updated.setNotifyPostDeleted(body.getOrDefault("notifyPostDeleted", false));
        updated.setNotifyMemberJoin(body.getOrDefault("notifyMemberJoin", true));
        updated.setNotifyMemberLeave(body.getOrDefault("notifyMemberLeave", false));

        DiscordChannelSettingsEntity settings = discordChannelService.updateSettings(projectId, updated);
        return ResponseEntity.ok(Map.of(
                "notifyPostCreated", settings.isNotifyPostCreated(),
                "notifyPostUpdated", settings.isNotifyPostUpdated(),
                "notifyPostDeleted", settings.isNotifyPostDeleted(),
                "notifyMemberJoin", settings.isNotifyMemberJoin(),
                "notifyMemberLeave", settings.isNotifyMemberLeave()
        ));
    }
}
