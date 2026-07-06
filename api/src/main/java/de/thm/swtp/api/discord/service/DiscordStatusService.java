package de.thm.swtp.api.discord.service;

import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import de.thm.swtp.api.discord.repository.DiscordMessageSyncRepository;
import de.thm.swtp.api.discord.repository.LinkedChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscordStatusService {

    private final LinkedChannelRepository linkedChannelRepository;
    private final DiscordMessageSyncRepository messageSyncRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getStatus(UUID projectId) {
        var linkOpt = linkedChannelRepository.findByProjectId(projectId);

        if (linkOpt.isEmpty()) {
            return Map.of(
                    "isActive", false,
                    "channelId", null,
                    "syncedToday", 0,
                    "failedToday", 0
            );
        }

        LinkedChannelEntity link = linkOpt.get();
        long syncedToday = messageSyncRepository.countBySyncedAtAfter(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));

        return Map.of(
                "isActive", link.isActive(),
                "channelId", link.getDiscordChannelId(),
                "syncedToday", syncedToday,
                "failedToday", 0
        );
    }
}
