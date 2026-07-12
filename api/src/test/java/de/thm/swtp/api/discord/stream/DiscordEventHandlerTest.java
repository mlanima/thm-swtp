package de.thm.swtp.api.discord.stream;

import de.thm.swtp.api.discord.entity.DiscordMessageSyncEntity;
import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import de.thm.swtp.api.discord.repository.DiscordMessageSyncRepository;
import de.thm.swtp.api.discord.repository.LinkedChannelRepository;
import de.thm.swtp.api.discord.stream.payload.ChannelDisconnectedPayload;
import de.thm.swtp.api.discord.stream.payload.DiscordMessageCreatedPayload;
import de.thm.swtp.api.discord.stream.payload.DiscordMessageDeletedPayload;
import de.thm.swtp.api.discord.stream.payload.DiscordMessageUpdatedPayload;
import de.thm.swtp.api.discord.stream.payload.InviteResponsePayload;
import de.thm.swtp.api.discord.stream.payload.MessageAssignedPayload;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.projectPost.domain.ProjectPostStatus;
import de.thm.swtp.api.projectPost.entity.ProjectPostEntity;
import de.thm.swtp.api.projectPost.repository.ProjectPostRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscordEventHandlerTest {

    @Mock
    private DiscordMessageSyncRepository messageSyncRepository;

    @Mock
    private LinkedChannelRepository linkedChannelRepository;

    @Mock
    private ProjectPostRepository projectPostRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private DiscordEventPublisher discordEventPublisher;

    private DiscordEventHandler handler;

    private UUID projectId;
    private UUID postId;
    private ProjectEntity project;
    private UserProfile author;
    private LinkedChannelEntity link;

    @BeforeEach
    void setUp() {
        handler = new DiscordEventHandler(messageSyncRepository, linkedChannelRepository,
                projectPostRepository, userProfileRepository, discordEventPublisher);

        projectId = UUID.randomUUID();
        postId = UUID.randomUUID();
        author = UserProfile.builder()
                .keycloakId(UUID.randomUUID())
                .username("Alice")
                .discordId("discord-user-1")
                .build();
        project = ProjectEntity.builder()
                .id(projectId)
                .name("Test Project")
                .owner(UserProfile.builder()
                        .keycloakId(UUID.randomUUID())
                        .username("Owner")
                        .discordId("discord-owner")
                        .build())
                .build();
        link = LinkedChannelEntity.builder()
                .id(UUID.randomUUID())
                .project(project)
                .discordChannelId("ch-1")
                .isActive(true)
                .build();
    }

    // ── handleDiscordMessageCreated ──

    @Test
    void shouldCreatePostFromDiscordMessage() {
        var payload = new DiscordMessageCreatedPayload(
                "discord-msg-1", "ch-1", "Hello from Discord",
                "discord-user-1", "Danny", null);

        when(messageSyncRepository.existsByDiscordMessageId("discord-msg-1")).thenReturn(false);
        when(linkedChannelRepository.findByDiscordChannelIdAndIsActiveTrue("ch-1")).thenReturn(Optional.of(link));
        when(userProfileRepository.findByDiscordId("discord-user-1")).thenReturn(Optional.of(author));

        var savedPost = ProjectPostEntity.builder()
                .id(postId)
                .project(project)
                .author(author)
                .build();
        when(projectPostRepository.save(any())).thenReturn(savedPost);

        handler.handleDiscordMessageCreated(payload);

        verify(projectPostRepository).save(any());
        var syncCaptor = ArgumentCaptor.<DiscordMessageSyncEntity>captor();
        verify(messageSyncRepository).save(syncCaptor.capture());
        assertThat(syncCaptor.getValue().getPlatformPostId()).isEqualTo(postId);
        assertThat(syncCaptor.getValue().getDirection()).isEqualTo(DiscordMessageSyncEntity.SyncDirection.DISCORD_TO_PLATFORM);
    }

    @Test
    void shouldSkipDuplicateDiscordMessage() {
        var payload = new DiscordMessageCreatedPayload(
                "discord-msg-dup", "ch-1", "dup", "user-1", "User", null);

        when(messageSyncRepository.existsByDiscordMessageId("discord-msg-dup")).thenReturn(true);

        handler.handleDiscordMessageCreated(payload);

        verify(projectPostRepository, never()).save(any());
    }

    @Test
    void shouldSkipWhenNoActiveLinkedChannel() {
        var payload = new DiscordMessageCreatedPayload(
                "msg-1", "unknown-ch", "content", "user-1", "User", null);

        when(messageSyncRepository.existsByDiscordMessageId("msg-1")).thenReturn(false);
        when(linkedChannelRepository.findByDiscordChannelIdAndIsActiveTrue("unknown-ch")).thenReturn(Optional.empty());

        handler.handleDiscordMessageCreated(payload);

        verify(projectPostRepository, never()).save(any());
    }

    @Test
    void shouldCreateGhostUserWhenDiscordUserNotFound() {
        var payload = new DiscordMessageCreatedPayload(
                "msg-ghost", "ch-1", "content", "unknown-user", "Ghost", null);

        when(messageSyncRepository.existsByDiscordMessageId("msg-ghost")).thenReturn(false);
        when(linkedChannelRepository.findByDiscordChannelIdAndIsActiveTrue("ch-1")).thenReturn(Optional.of(link));
        when(userProfileRepository.findByDiscordId("unknown-user")).thenReturn(Optional.empty());

        var savedGhost = UserProfile.builder()
                .keycloakId(UUID.randomUUID())
                .username("Ghost#discord")
                .build();
        when(userProfileRepository.save(any())).thenReturn(savedGhost);

        var savedPost = ProjectPostEntity.builder()
                .id(UUID.randomUUID())
                .project(project)
                .author(savedGhost)
                .build();
        when(projectPostRepository.save(any())).thenReturn(savedPost);

        handler.handleDiscordMessageCreated(payload);

        var userCaptor = ArgumentCaptor.<UserProfile>captor();
        verify(userProfileRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).endsWith("#discord");
    }

    @Test
    void shouldHandleDataIntegrityViolationOnDuplicate() {
        var payload = new DiscordMessageCreatedPayload(
                "msg-race", "ch-1", "content", "user-1", "User", null);

        when(messageSyncRepository.existsByDiscordMessageId("msg-race")).thenReturn(false);
        when(linkedChannelRepository.findByDiscordChannelIdAndIsActiveTrue("ch-1")).thenReturn(Optional.of(link));
        when(userProfileRepository.findByDiscordId("user-1")).thenReturn(Optional.of(author));
        when(projectPostRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        handler.handleDiscordMessageCreated(payload);

        verify(messageSyncRepository, never()).save(any());
    }

    // ── handleDiscordMessageUpdated ──

    @Test
    void shouldUpdatePostFromDiscord() {
        var sync = DiscordMessageSyncEntity.builder()
                .platformPostId(postId)
                .discordMessageId("msg-1")
                .build();
        var existingPost = ProjectPostEntity.builder()
                .id(postId)
                .content("old content")
                .build();

        when(messageSyncRepository.findByDiscordMessageId("msg-1")).thenReturn(Optional.of(sync));
        when(projectPostRepository.findById(postId)).thenReturn(Optional.of(existingPost));

        handler.handleDiscordMessageUpdated(new DiscordMessageUpdatedPayload("msg-1", "new content"));

        assertThat(existingPost.getContent()).isEqualTo("new content");
        verify(projectPostRepository).save(existingPost);
    }

    @Test
    void shouldDoNothingWhenNoSyncRecord() {
        when(messageSyncRepository.findByDiscordMessageId("unknown")).thenReturn(Optional.empty());

        handler.handleDiscordMessageUpdated(new DiscordMessageUpdatedPayload("unknown", "content"));

        verify(projectPostRepository, never()).save(any());
    }

    // ── handleDiscordMessageDeleted ──

    @Test
    void shouldDeletePostFromDiscord() {
        var postEntity = ProjectPostEntity.builder().id(postId).build();
        var sync = DiscordMessageSyncEntity.builder()
                .platformPostId(postId)
                .discordMessageId("msg-1")
                .build();

        when(messageSyncRepository.findByDiscordMessageId("msg-1")).thenReturn(Optional.of(sync));
        when(projectPostRepository.findById(postId)).thenReturn(Optional.of(postEntity));

        handler.handleDiscordMessageDeleted(new DiscordMessageDeletedPayload("msg-1"));

        verify(projectPostRepository).delete(postEntity);
        verify(messageSyncRepository).delete(sync);
    }

    // ── handleMessageAssigned ──

    @Test
    void shouldCreateSyncFromAssignedMessage() {
        var payload = new MessageAssignedPayload(
                postId, "discord-msg-1", "ch-1", "guild-1");

        when(messageSyncRepository.existsByDiscordMessageId("discord-msg-1")).thenReturn(false);
        when(projectPostRepository.existsById(postId)).thenReturn(true);
        when(messageSyncRepository.findByPlatformPostId(postId)).thenReturn(Optional.empty());

        handler.handleMessageAssigned(payload);

        var captor = ArgumentCaptor.<DiscordMessageSyncEntity>captor();
        verify(messageSyncRepository).save(captor.capture());
        assertThat(captor.getValue().getDirection()).isEqualTo(DiscordMessageSyncEntity.SyncDirection.PLATFORM_TO_DISCORD);
    }

    @Test
    void shouldSkipAssignedMessageWhenDuplicate() {
        var payload = new MessageAssignedPayload(postId, "dup-msg", "ch-1", "guild-1");

        when(messageSyncRepository.existsByDiscordMessageId("dup-msg")).thenReturn(true);

        handler.handleMessageAssigned(payload);

        verify(messageSyncRepository, never()).save(any());
    }

    @Test
    void shouldSaveSyncWhenPostNotPublished() {
        var payload = new MessageAssignedPayload(postId, "orphan-msg", "ch-1", "guild-1");

        when(messageSyncRepository.existsByDiscordMessageId("orphan-msg")).thenReturn(false);
        when(projectPostRepository.existsById(postId)).thenReturn(true);
        when(messageSyncRepository.findByPlatformPostId(postId)).thenReturn(Optional.empty());

        handler.handleMessageAssigned(payload);

        verify(discordEventPublisher, never()).publishDirectDelete(any(), any(), any());

        var captor = ArgumentCaptor.<DiscordMessageSyncEntity>captor();
        verify(messageSyncRepository).save(captor.capture());
        var savedSync = captor.getValue();
        assertThat(savedSync.getPlatformPostId()).isEqualTo(postId);
        assertThat(savedSync.getDiscordMessageId()).isEqualTo("orphan-msg");
        assertThat(savedSync.getDirection()).isEqualTo(DiscordMessageSyncEntity.SyncDirection.PLATFORM_TO_DISCORD);

        verify(messageSyncRepository, never()).delete(any());
    }

    @Test
    void shouldSkipAssignedMessageWhenPostNotFoundInDb() {
        var payload = new MessageAssignedPayload(postId, "cross-env-msg", "ch-1", "guild-1");

        when(messageSyncRepository.existsByDiscordMessageId("cross-env-msg")).thenReturn(false);
        when(projectPostRepository.existsById(postId)).thenReturn(false);

        handler.handleMessageAssigned(payload);

        verify(messageSyncRepository, never()).save(any());
        verify(discordEventPublisher, never()).publishDirectDelete(any(), any(), any());
    }

    // ── handleInviteResponse ──

    @Test
    void shouldHandleInviteResponse() {
        handler.handleInviteResponse(new InviteResponsePayload("invite-1", "ACCEPTED"));

        // just verify no exception is thrown
    }

    // ── handleChannelDisconnected ──

    @Test
    void shouldDeactivateChannelOnDisconnect() {
        when(linkedChannelRepository.findByDiscordChannelId("ch-1")).thenReturn(Optional.of(link));

        handler.handleChannelDisconnected(new ChannelDisconnectedPayload("ch-1", "bot_left"));

        assertThat(link.isActive()).isFalse();
        verify(linkedChannelRepository).save(link);
    }

    @Test
    void shouldDoNothingWhenChannelNotFound() {
        when(linkedChannelRepository.findByDiscordChannelId("unknown-ch")).thenReturn(Optional.empty());

        handler.handleChannelDisconnected(new ChannelDisconnectedPayload("unknown-ch", "reason"));

        verify(linkedChannelRepository, never()).save(any());
    }
}
