package de.thm.swtp.api.discord.repository;

import de.thm.swtp.api.discord.entity.DiscordChannelSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Per-channel notification preferences for a linked Discord channel.
 */
public interface DiscordChannelSettingsRepository extends JpaRepository<DiscordChannelSettingsEntity, UUID> {

    /** Load settings for a specific channel link. */
    Optional<DiscordChannelSettingsEntity> findByLinkedChannelId(UUID linkedChannelId);
}
