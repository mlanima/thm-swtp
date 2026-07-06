package de.thm.swtp.api.discord.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "discord_message_sync", uniqueConstraints = {
        @UniqueConstraint(name = "UK_discord_message_sync_discord_msg", columnNames = {"discord_message_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscordMessageSyncEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "platform_post_id", nullable = false)
    private UUID platformPostId;

    @Column(name = "discord_message_id", nullable = false, unique = true, length = 20)
    private String discordMessageId;

    @Column(name = "discord_channel_id", nullable = false, length = 20)
    private String discordChannelId;

    @Column(name = "discord_guild_id", length = 20)
    private String discordGuildId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    @Builder.Default
    private SyncDirection direction = SyncDirection.PLATFORM_TO_DISCORD;

    @CreationTimestamp
    @Column(name = "synced_at", nullable = false, updatable = false)
    private LocalDateTime syncedAt;

    public enum SyncDirection {
        PLATFORM_TO_DISCORD,
        DISCORD_TO_PLATFORM
    }
}
