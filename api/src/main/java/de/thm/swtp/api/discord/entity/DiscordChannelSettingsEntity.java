package de.thm.swtp.api.discord.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "discord_channel_settings", uniqueConstraints = {
        @UniqueConstraint(name = "UK_discord_channel_settings_link", columnNames = {"linked_channel_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscordChannelSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "linked_channel_id", nullable = false, unique = true)
    private LinkedChannelEntity linkedChannel;

    @Column(name = "notify_post_created", nullable = false)
    @Builder.Default
    private boolean notifyPostCreated = true;

    @Column(name = "notify_post_updated", nullable = false)
    @Builder.Default
    private boolean notifyPostUpdated = true;

    @Column(name = "notify_post_deleted", nullable = false)
    @Builder.Default
    private boolean notifyPostDeleted = false;

    @Column(name = "notify_member_join", nullable = false)
    @Builder.Default
    private boolean notifyMemberJoin = true;

    @Column(name = "notify_member_leave", nullable = false)
    @Builder.Default
    private boolean notifyMemberLeave = false;

    public boolean shouldNotify(String eventType) {
        return switch (eventType) {
            case "MEMBER_JOIN" -> notifyMemberJoin;
            case "MEMBER_LEAVE" -> notifyMemberLeave;
            default -> true;
        };
    }

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
