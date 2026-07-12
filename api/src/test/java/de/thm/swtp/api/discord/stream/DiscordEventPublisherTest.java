package de.thm.swtp.api.discord.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.thm.swtp.api.discord.config.DiscordProperties;
import de.thm.swtp.api.discord.entity.DiscordChannelSettingsEntity;
import de.thm.swtp.api.discord.entity.DiscordMessageSyncEntity;
import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import de.thm.swtp.api.discord.repository.DiscordChannelSettingsRepository;
import de.thm.swtp.api.discord.repository.DiscordMessageSyncRepository;
import de.thm.swtp.api.discord.repository.LinkedChannelRepository;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.projectPost.entity.ProjectPostEntity;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscordEventPublisherTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private DiscordProperties discordProperties;

    @Mock
    private LinkedChannelRepository linkedChannelRepository;

    @Mock
    private DiscordMessageSyncRepository messageSyncRepository;

    @Mock
    private DiscordChannelSettingsRepository settingsRepository;

    @Mock
    private PostUrlBuilder postUrlBuilder;

    @Mock
    private DiscordProperties.Streams streams;

    @Mock
    private StreamOperations<String, Object, Object> streamOps;

    private DiscordEventPublisher publisher;

    private UUID projectId;
    private UUID postId;
    private ProjectEntity project;
    private UserProfile author;
    private ProjectPostEntity post;
    private LinkedChannelEntity link;

    @BeforeEach
    void setUp() {
        publisher = new DiscordEventPublisher(redis, discordProperties, linkedChannelRepository,
                messageSyncRepository, settingsRepository, postUrlBuilder);

        projectId = UUID.randomUUID();
        postId = UUID.randomUUID();
        author = UserProfile.builder()
                .keycloakId(UUID.randomUUID())
                .username("Alice")
                .discordAvatar("avatar_hash")
                .build();
        project = ProjectEntity.builder()
                .id(projectId)
                .name("Test Project")
                .projectUrl("test-project")
                .owner(UserProfile.builder()
                        .keycloakId(UUID.randomUUID())
                        .username("Owner")
                        .build())
                .build();
        post = ProjectPostEntity.builder()
                .id(postId)
                .project(project)
                .author(author)
                .title("Post Title")
                .content("Post content")
                .build();
        link = LinkedChannelEntity.builder()
                .id(UUID.randomUUID())
                .project(project)
                .discordChannelId("ch-1")
                .isActive(true)
                .build();

        lenient().when(discordProperties.getStreams()).thenReturn(streams);
        lenient().when(streams.getOutbound()).thenReturn("stream:discord:sync");
        lenient().when(redis.opsForStream()).thenReturn(streamOps);
    }

    // ── publishPostCreated ──

    @Test
    void shouldPublishPostCreated() {
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.of(link));
        when(postUrlBuilder.buildPostUrl(post)).thenReturn("http://localhost:4200/project/test-project");

        publisher.publishPostCreated(post);

        var captor = ArgumentCaptor.<Map<String, String>>captor();
        verify(streamOps).add(eq("stream:discord:sync"), captor.capture());

        var record = captor.getValue();
        assertThat(record).containsEntry("type", "POST_CREATED");
        assertThat(record.get("payload")).contains("\"postId\":\"" + postId + "\"");
        assertThat(record.get("payload")).contains("\"authorAvatar\":\"avatar_hash\"");
    }

    @Test
    void shouldSkipPublishPostCreatedWhenNoActiveChannel() {
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.empty());

        publisher.publishPostCreated(post);

        verify(streamOps, never()).add(anyString(), anyMap());
    }

    @Test
    void shouldSkipAvatarWhenBlank() {
        author.setDiscordAvatar(null);
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.of(link));
        when(postUrlBuilder.buildPostUrl(post)).thenReturn("http://localhost:4200/project/test-project");

        publisher.publishPostCreated(post);

        var captor = ArgumentCaptor.<Map<String, String>>captor();
        verify(streamOps).add(eq("stream:discord:sync"), captor.capture());

        assertThat(captor.getValue().get("payload")).doesNotContain("authorAvatar");
    }

    @Test
    void shouldTruncateLongContentOnPostCreated() {
        var longContent = "x".repeat(5000);
        post.setContent(longContent);
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.of(link));
        when(postUrlBuilder.buildPostUrl(post)).thenReturn("http://localhost:4200/project/test-project");

        publisher.publishPostCreated(post);

        var captor = ArgumentCaptor.<Map<String, String>>captor();
        verify(streamOps).add(eq("stream:discord:sync"), captor.capture());

        var payload = captor.getValue().get("payload");
        assertThat(payload).contains("\"content\":\"" + "x".repeat(3897) + "...\"");
    }

    // ── publishPostUpdated ──

    @Test
    void shouldPublishPostUpdated() {
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.of(link));

        publisher.publishPostUpdated(post, "discord-msg-1");

        var captor = ArgumentCaptor.<Map<String, String>>captor();
        verify(streamOps).add(eq("stream:discord:sync"), captor.capture());

        var record = captor.getValue();
        assertThat(record).containsEntry("type", "POST_UPDATED");
        assertThat(record.get("payload")).contains("\"discordMsgId\":\"discord-msg-1\"");
    }

    @Test
    void shouldSkipPublishPostUpdatedWhenNoActiveChannel() {
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.empty());

        publisher.publishPostUpdated(post, "msg-1");

        verify(streamOps, never()).add(anyString(), anyMap());
    }

    // ── publishPostDeleted ──

    @Test
    void shouldPublishPostDeleted() {
        var sync = DiscordMessageSyncEntity.builder()
                .discordMessageId("discord-msg-1")
                .discordChannelId("ch-1")
                .build();
        when(messageSyncRepository.findByDiscordMessageId("discord-msg-1")).thenReturn(Optional.of(sync));

        publisher.publishPostDeleted(postId, "discord-msg-1");

        var captor = ArgumentCaptor.<Map<String, String>>captor();
        verify(streamOps).add(eq("stream:discord:sync"), captor.capture());

        var record = captor.getValue();
        assertThat(record).containsEntry("type", "POST_DELETED");
    }

    @Test
    void shouldSkipPublishPostDeletedWhenNoSyncRecord() {
        when(messageSyncRepository.findByDiscordMessageId("unknown-msg")).thenReturn(Optional.empty());

        publisher.publishPostDeleted(postId, "unknown-msg");

        verify(streamOps, never()).add(anyString(), anyMap());
    }

    // ── publishDirectDelete ──

    @Test
    void shouldPublishDirectDelete() {
        publisher.publishDirectDelete(postId, "discord-msg-1", "ch-1");

        var captor = ArgumentCaptor.<Map<String, String>>captor();
        verify(streamOps).add(eq("stream:discord:sync"), captor.capture());

        assertThat(captor.getValue()).containsEntry("type", "POST_DELETED");
    }

    // ── publishProjectInvite ──

    @Test
    void shouldPublishProjectInvite() {
        publisher.publishProjectInvite(UUID.randomUUID(), "target-123", "My Project", "Inviter");

        var captor = ArgumentCaptor.<Map<String, String>>captor();
        verify(streamOps).add(eq("stream:discord:sync"), captor.capture());

        assertThat(captor.getValue()).containsEntry("type", "PROJECT_INVITE");
    }

    // ── publishProjectEvent ──

    @Test
    void shouldPublishProjectEvent() {
        var settings = DiscordChannelSettingsEntity.builder()
                .linkedChannel(link)
                .notifyMemberJoin(true)
                .build();
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.of(link));
        when(settingsRepository.findByLinkedChannelId(link.getId())).thenReturn(Optional.of(settings));

        publisher.publishProjectEvent(projectId, "MEMBER_JOIN", "Alice joined");

        var captor = ArgumentCaptor.<Map<String, String>>captor();
        verify(streamOps).add(eq("stream:discord:sync"), captor.capture());

        var record = captor.getValue();
        assertThat(record).containsEntry("type", "PROJECT_EVENT");
        assertThat(record.get("payload")).contains("\"eventType\":\"MEMBER_JOIN\"");
    }

    @Test
    void shouldSkipPublishProjectEventWhenNoActiveChannel() {
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.empty());

        publisher.publishProjectEvent(projectId, "MEMBER_JOIN", "msg");

        verify(streamOps, never()).add(anyString(), anyMap());
    }

    @Test
    void shouldSkipPublishProjectEventWhenNotifyDisabled() {
        var settings = DiscordChannelSettingsEntity.builder()
                .linkedChannel(link)
                .notifyMemberJoin(false)
                .build();
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.of(link));
        when(settingsRepository.findByLinkedChannelId(link.getId())).thenReturn(Optional.of(settings));

        publisher.publishProjectEvent(projectId, "MEMBER_JOIN", "msg");

        verify(streamOps, never()).add(anyString(), anyMap());
    }

    @Test
    void shouldTruncateLongMessageOnProjectEvent() {
        var settings = DiscordChannelSettingsEntity.builder()
                .linkedChannel(link)
                .notifyMemberJoin(true)
                .build();
        var longMsg = "y".repeat(5000);
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.of(link));
        when(settingsRepository.findByLinkedChannelId(link.getId())).thenReturn(Optional.of(settings));

        publisher.publishProjectEvent(projectId, "MEMBER_JOIN", longMsg);

        var captor = ArgumentCaptor.<Map<String, String>>captor();
        verify(streamOps).add(eq("stream:discord:sync"), captor.capture());

        var payload = captor.getValue().get("payload");
        assertThat(payload).contains("\"message\":\"" + "y".repeat(3897) + "...\"");
    }

    // ── send error handling ──

    @Test
    void shouldLogErrorWhenSendFails() {
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.of(link));
        when(postUrlBuilder.buildPostUrl(post)).thenReturn("http://localhost:4200/project/test-project");
        when(streamOps.add(anyString(), anyMap())).thenThrow(new RuntimeException("Redis down"));

        publisher.publishPostCreated(post);

        verify(streamOps).add(anyString(), anyMap());
    }

    // ── truncate utility ──

    @Test
    void shouldNotTruncateShortContent() {
        var shortContent = "x".repeat(100);
        post.setContent(shortContent);
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.of(link));
        when(postUrlBuilder.buildPostUrl(post)).thenReturn("http://localhost:4200/project/test-project");

        publisher.publishPostCreated(post);

        var captor = ArgumentCaptor.<Map<String, String>>captor();
        verify(streamOps).add(eq("stream:discord:sync"), captor.capture());

        var payload = captor.getValue().get("payload");
        assertThat(payload).contains("\"content\":\"" + shortContent + "\"");
    }

    @Test
    void shouldHandleNullContent() {
        post.setContent(null);
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.of(link));
        when(postUrlBuilder.buildPostUrl(post)).thenReturn("http://localhost:4200/project/test-project");

        publisher.publishPostCreated(post);

        var captor = ArgumentCaptor.<Map<String, String>>captor();
        verify(streamOps).add(eq("stream:discord:sync"), captor.capture());

        var payload = captor.getValue().get("payload");
        assertThat(payload).contains("\"content\":null");
    }
}
