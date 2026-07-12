package de.thm.swtp.api.discord.repository;

import de.thm.swtp.api.discord.entity.DiscordMessageSyncEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Tracks which platform posts have been mirrored to Discord and vice versa.
 */
public interface DiscordMessageSyncRepository extends JpaRepository<DiscordMessageSyncEntity, UUID> {

    /** True if a Discord message has already been imported (dedup check). */
    boolean existsByDiscordMessageId(String discordMessageId);

    /** Looks up the sync record for a given platform post. */
    Optional<DiscordMessageSyncEntity> findByPlatformPostId(UUID platformPostId);

    /** Looks up the sync record by its Discord message ID. */
    Optional<DiscordMessageSyncEntity> findByDiscordMessageId(String discordMessageId);

    /** How many messages were synced since a given timestamp. */
    long countBySyncedAtAfter(LocalDateTime since);

    /** Sync count for a specific channel since a timestamp. */
    long countByDiscordChannelIdAndSyncedAtAfter(String discordChannelId, LocalDateTime since);

    void deleteByPlatformPostId(UUID platformPostId);
}
