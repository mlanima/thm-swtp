package de.thm.swtp.api.discord.repository;

import de.thm.swtp.api.discord.entity.DiscordMessageSyncEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface DiscordMessageSyncRepository extends JpaRepository<DiscordMessageSyncEntity, UUID> {

    boolean existsByDiscordMessageId(String discordMessageId);

    Optional<DiscordMessageSyncEntity> findByPlatformPostId(UUID platformPostId);

    Optional<DiscordMessageSyncEntity> findByDiscordMessageId(String discordMessageId);

    long countBySyncedAtAfter(LocalDateTime since);

    long countByDiscordChannelIdAndSyncedAtAfter(String discordChannelId, LocalDateTime since);

    void deleteByPlatformPostId(UUID platformPostId);
}
