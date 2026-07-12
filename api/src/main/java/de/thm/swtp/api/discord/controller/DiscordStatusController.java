package de.thm.swtp.api.discord.controller;

import de.thm.swtp.api.discord.dto.DiscordStatusResponse;
import de.thm.swtp.api.discord.service.DiscordStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Returns the current Discord integration status for a project
 * (connection state, bot presence, channel info).
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/discord/status")
@RequiredArgsConstructor
public class DiscordStatusController {

    private final DiscordStatusService discordStatusService;

    @GetMapping
    @PreAuthorize("@security.canViewProject(#projectId, authentication)")
    public ResponseEntity<DiscordStatusResponse> getStatus(@PathVariable UUID projectId) {
        return ResponseEntity.ok(discordStatusService.getStatus(projectId));
    }
}
