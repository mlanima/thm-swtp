package de.thm.swtp.api.discord.controller;

import de.thm.swtp.api.discord.service.DiscordStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/discord/status")
@RequiredArgsConstructor
public class DiscordStatusController {

    private final DiscordStatusService discordStatusService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable UUID projectId) {
        return ResponseEntity.ok(discordStatusService.getStatus(projectId));
    }
}
