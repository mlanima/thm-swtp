package de.thm.swtp.api.discord.service;

import de.thm.swtp.api.discord.dto.DiscordStatusResponse;
import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import de.thm.swtp.api.discord.repository.DiscordMessageSyncRepository;
import de.thm.swtp.api.discord.repository.LinkedChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reports the Discord connection status for a project:
 * whether a channel is linked and how many messages were synced today.
 */
@Service
@RequiredArgsConstructor
public class DiscordStatusService {

    private final LinkedChannelRepository linkedChannelRepository;
    private final DiscordMessageSyncRepository messageSyncRepository;

    /**
     * Returns the current Discord link status and today's sync count.
     */
    @Transactional(readOnly = true)
    public DiscordStatusResponse getStatus(UUID projectId) {
        var linkOpt = linkedChannelRepository.findByProjectId(projectId);

        if (linkOpt.isEmpty()) {
            return new DiscordStatusResponse(false, null, 0, 0);
        }

        LinkedChannelEntity link = linkOpt.get();
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long syncedToday = messageSyncRepository.countByDiscordChannelIdAndSyncedAtAfter(
                link.getDiscordChannelId(), todayStart);

        return new DiscordStatusResponse(
                link.isActive(),
                link.getDiscordChannelId(),
                syncedToday,
                0
        );
    }
}
