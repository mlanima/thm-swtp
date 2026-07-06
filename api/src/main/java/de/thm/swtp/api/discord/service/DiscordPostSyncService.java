package de.thm.swtp.api.discord.service;

import de.thm.swtp.api.discord.entity.DiscordMessageSyncEntity;
import de.thm.swtp.api.discord.repository.DiscordMessageSyncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscordPostSyncService {

    private final DiscordMessageSyncRepository messageSyncRepository;

    @Transactional(readOnly = true)
    public Optional<String> getDiscordMessageId(UUID postId) {
        return messageSyncRepository.findByPlatformPostId(postId)
                .map(DiscordMessageSyncEntity::getDiscordMessageId);
    }

    @Transactional(readOnly = true)
    public boolean isSynced(UUID postId) {
        return messageSyncRepository.findByPlatformPostId(postId).isPresent();
    }

    @Transactional(readOnly = true)
    public Optional<UUID> getPlatformPostId(String discordMessageId) {
        return messageSyncRepository.findByDiscordMessageId(discordMessageId)
                .map(DiscordMessageSyncEntity::getPlatformPostId);
    }

    @Transactional
    public void saveSync(UUID postId, String discordMessageId, String channelId,
                         DiscordMessageSyncEntity.SyncDirection direction) {
        DiscordMessageSyncEntity sync = DiscordMessageSyncEntity.builder()
                .platformPostId(postId)
                .discordMessageId(discordMessageId)
                .discordChannelId(channelId)
                .direction(direction)
                .build();
        messageSyncRepository.save(sync);
        log.debug("Sync mapping saved: postId={}, discordMsgId={}", postId, discordMessageId);
    }

    @Transactional
    public void removeSync(UUID postId) {
        messageSyncRepository.findByPlatformPostId(postId)
                .ifPresent(messageSyncRepository::delete);
    }
}
