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

@Service
@RequiredArgsConstructor
public class DiscordStatusService {

    private final LinkedChannelRepository linkedChannelRepository;
    private final DiscordMessageSyncRepository messageSyncRepository;

    @Transactional(readOnly = true)
    public DiscordStatusResponse getStatus(UUID projectId) {
        var linkOpt = linkedChannelRepository.findByProjectId(projectId);

        if (linkOpt.isEmpty()) {
            return new DiscordStatusResponse(false, null, 0, 0);
        }

        LinkedChannelEntity link = linkOpt.get();
        long syncedToday = messageSyncRepository.countBySyncedAtAfter(
                LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));

        return new DiscordStatusResponse(
                link.isActive(),
                link.getDiscordChannelId(),
                syncedToday,
                0
        );
    }
}
