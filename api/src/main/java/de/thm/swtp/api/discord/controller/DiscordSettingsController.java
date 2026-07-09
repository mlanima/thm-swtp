package de.thm.swtp.api.discord.controller;

import de.thm.swtp.api.discord.dto.DiscordSettingsResponse;
import de.thm.swtp.api.discord.entity.DiscordChannelSettingsEntity;
import de.thm.swtp.api.discord.service.DiscordChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/discord/settings")
@RequiredArgsConstructor
public class DiscordSettingsController {

    private final DiscordChannelService discordChannelService;

    @GetMapping
    @PreAuthorize("@security.canViewProject(#projectId, authentication)")
    public ResponseEntity<DiscordSettingsResponse> getSettings(@PathVariable UUID projectId) {
        DiscordChannelSettingsEntity settings = discordChannelService.getSettings(projectId);
        return ResponseEntity.ok(DiscordSettingsResponse.from(settings));
    }

    @PutMapping
    @PreAuthorize("@security.canEditProject(#projectId, authentication)")
    public ResponseEntity<DiscordSettingsResponse> updateSettings(
            @PathVariable UUID projectId,
            @RequestBody DiscordSettingsResponse.UpdateRequest body) {
        DiscordChannelSettingsEntity updated = new DiscordChannelSettingsEntity();
        updated.setNotifyPostCreated(body.notifyPostCreated());
        updated.setNotifyPostUpdated(body.notifyPostUpdated());
        updated.setNotifyPostDeleted(body.notifyPostDeleted());
        updated.setNotifyMemberJoin(body.notifyMemberJoin());
        updated.setNotifyMemberLeave(body.notifyMemberLeave());

        DiscordChannelSettingsEntity settings = discordChannelService.updateSettings(projectId, updated);
        return ResponseEntity.ok(DiscordSettingsResponse.from(settings));
    }
}
