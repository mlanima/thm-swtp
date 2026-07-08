package de.thm.swtp.api.discord.repository;

import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkedChannelRepository extends JpaRepository<LinkedChannelEntity, UUID> {

    Optional<LinkedChannelEntity> findByProjectId(UUID projectId);

    Optional<LinkedChannelEntity> findByDiscordChannelId(String discordChannelId);

    Optional<LinkedChannelEntity> findByDiscordChannelIdAndIsActiveTrue(String discordChannelId);

    List<LinkedChannelEntity> findAllByDiscordGuildIdAndIsActiveTrue(String discordGuildId);
}
