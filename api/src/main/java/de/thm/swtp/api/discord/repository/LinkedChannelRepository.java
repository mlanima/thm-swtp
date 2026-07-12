package de.thm.swtp.api.discord.repository;

import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Maps Discord channels to platform projects.
 */
public interface LinkedChannelRepository extends JpaRepository<LinkedChannelEntity, UUID> {

    /** Looks up a channel link by the platform project it belongs to. */
    Optional<LinkedChannelEntity> findByProjectId(UUID projectId);

    /** Looks up a channel link by its Discord channel ID. */
    Optional<LinkedChannelEntity> findByDiscordChannelId(String discordChannelId);

    /** Same as above, but only returns the link if it's still active. */
    Optional<LinkedChannelEntity> findByDiscordChannelIdAndIsActiveTrue(String discordChannelId);

    /** All active channel links within a Discord server. */
    List<LinkedChannelEntity> findAllByDiscordGuildIdAndIsActiveTrue(String discordGuildId);

    void deleteByProjectId(UUID projectId);
}
