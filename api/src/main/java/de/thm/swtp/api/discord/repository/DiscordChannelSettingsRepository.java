package de.thm.swtp.api.discord.repository;

import de.thm.swtp.api.discord.entity.DiscordChannelSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DiscordChannelSettingsRepository extends JpaRepository<DiscordChannelSettingsEntity, UUID> {

    Optional<DiscordChannelSettingsEntity> findByLinkedChannelId(UUID linkedChannelId);

    void deleteByLinkedChannelId(UUID linkedChannelId);
}
