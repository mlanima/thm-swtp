package de.thm.swtp.api.discord.service;

import de.thm.swtp.api.discord.client.BotOperations;
import de.thm.swtp.api.discord.client.BotOperations.AutoSetupResponse;
import de.thm.swtp.api.discord.client.BotOperations.CreateInviteResponse;
import de.thm.swtp.api.discord.client.BotOperations.GuildInfo;
import de.thm.swtp.api.discord.client.BotOperations.GuildsResponse;
import de.thm.swtp.api.discord.client.BotOperations.LeaveGuildResponse;
import de.thm.swtp.api.discord.client.BotOperations.TestConnectionResponse;
import de.thm.swtp.api.discord.config.DiscordProperties;
import de.thm.swtp.api.discord.dto.DiscordChannelResponse;
import de.thm.swtp.api.discord.entity.DiscordChannelSettingsEntity;
import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import de.thm.swtp.api.discord.exception.DiscordConnectionFailedException;
import de.thm.swtp.api.discord.repository.DiscordChannelSettingsRepository;
import de.thm.swtp.api.discord.repository.LinkedChannelRepository;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class DiscordChannelServiceTest {

    @Mock
    private LinkedChannelRepository linkedChannelRepository;

    @Mock
    private DiscordChannelSettingsRepository settingsRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private BotOperations botClient;

    @Mock
    private DiscordAuthService discordAuthService;

    @Mock
    private DiscordProperties discordProperties;

    private DiscordChannelService service;

    private UUID projectId;
    private UUID userId;
    private ProjectEntity project;
    private UserProfile owner;

    @BeforeEach
    void setUp() {
        service = new DiscordChannelService(linkedChannelRepository, settingsRepository,
                projectRepository, botClient, discordAuthService, discordProperties);

        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        owner = UserProfile.builder()
                .keycloakId(userId)
                .username("Owner")
                .discordId("discord-owner-123")
                .build();
        project = ProjectEntity.builder()
                .id(projectId)
                .name("Test Project")
                .owner(owner)
                .build();
    }

    // ── connectChannel ──

    @Test
    void shouldConnectChannel() {
        var channelId = "123456789";
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.empty());
        when(linkedChannelRepository.findByDiscordChannelIdAndIsActiveTrue(channelId)).thenReturn(Optional.empty());
        when(botClient.testConnection(channelId, null)).thenReturn(new TestConnectionResponse(true, null));
        when(botClient.createChannelInvite(channelId)).thenReturn(new CreateInviteResponse(true, "https://discord.gg/abc", null));

        var captor = ArgumentCaptor.<LinkedChannelEntity>captor();
        when(linkedChannelRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.connectChannel(projectId, channelId);

        assertThat(result.getDiscordChannelId()).isEqualTo(channelId);
        assertThat(result.isActive()).isTrue();
        assertThat(result.getDiscordInviteUrl()).isEqualTo("https://discord.gg/abc");
        verify(botClient).testConnection(channelId, null);
        verify(botClient).createChannelInvite(channelId);
        verify(settingsRepository).findByLinkedChannelId(captor.getValue().getId());
    }

    @Test
    void shouldThrowWhenProjectNotFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.connectChannel(projectId, "ch"))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void shouldThrowWhenOwnerNotLinked() {
        owner.setDiscordId(null);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service.connectChannel(projectId, "ch"))
                .isInstanceOf(DiscordConnectionFailedException.class)
                .hasMessageContaining("link your Discord account");
    }

    @Test
    void shouldThrowWhenChannelAlreadyLinkedToOtherProject() {
        var channelId = "123456789";
        var otherProject = ProjectEntity.builder().id(UUID.randomUUID()).build();
        var existingLink = LinkedChannelEntity.builder()
                .project(otherProject)
                .isActive(true)
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.empty());
        when(linkedChannelRepository.findByDiscordChannelIdAndIsActiveTrue(channelId))
                .thenReturn(Optional.of(existingLink));

        assertThatThrownBy(() -> service.connectChannel(projectId, channelId))
                .isInstanceOf(DiscordConnectionFailedException.class)
                .hasMessageContaining("already linked");
    }

    @Test
    void shouldThrowWhenBotConnectionFails() {
        var channelId = "123456789";
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.empty());
        when(linkedChannelRepository.findByDiscordChannelIdAndIsActiveTrue(channelId)).thenReturn(Optional.empty());
        when(botClient.testConnection(channelId, null))
                .thenReturn(new TestConnectionResponse(false, "bot unreachable"));

        assertThatThrownBy(() -> service.connectChannel(projectId, channelId))
                .isInstanceOf(DiscordConnectionFailedException.class)
                .hasMessageContaining("Bot cannot access channel");
    }

    @Test
    void shouldReuseExistingInactiveLink() {
        var channelId = "123456789";
        var existingLink = LinkedChannelEntity.builder()
                .id(UUID.randomUUID())
                .project(project)
                .discordChannelId("old-channel")
                .isActive(false)
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.of(existingLink));
        when(linkedChannelRepository.findByDiscordChannelIdAndIsActiveTrue(channelId)).thenReturn(Optional.empty());
        when(botClient.testConnection(channelId, null)).thenReturn(new TestConnectionResponse(true, null));
        when(botClient.createChannelInvite(channelId)).thenReturn(new CreateInviteResponse(false, null, null));

        when(linkedChannelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.connectChannel(projectId, channelId);

        assertThat(result.getDiscordChannelId()).isEqualTo(channelId);
        assertThat(result.isActive()).isTrue();
        assertThat(result.getDiscordInviteUrl()).isNull();
    }

    @Test
    void shouldDeactivateOldChannelOnReconnect() {
        var channelId = "123456789";
        var oldLink = LinkedChannelEntity.builder()
                .id(UUID.randomUUID())
                .discordChannelId("old-channel")
                .project(project)
                .isActive(true)
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.of(oldLink), Optional.of(oldLink));
        when(linkedChannelRepository.findByDiscordChannelIdAndIsActiveTrue(channelId)).thenReturn(Optional.empty());
        when(botClient.testConnection(channelId, null)).thenReturn(new TestConnectionResponse(true, null));
        when(botClient.createChannelInvite(channelId)).thenReturn(new CreateInviteResponse(false, null, null));
        when(linkedChannelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.connectChannel(projectId, channelId);

        assertThat(oldLink.isActive()).isFalse();
    }

    @Test
    void shouldDeactivateCrossProjectGuildLinks() {
        var channelId = "123456789";
        var guildId = "guild-1";
        var otherProject = ProjectEntity.builder().id(UUID.randomUUID()).build();
        var crossLink = LinkedChannelEntity.builder()
                .id(UUID.randomUUID())
                .project(otherProject)
                .discordGuildId(guildId)
                .isActive(true)
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.empty());
        when(linkedChannelRepository.findAllByDiscordGuildIdAndIsActiveTrue(guildId))
                .thenReturn(List.of(crossLink));
        when(linkedChannelRepository.findByDiscordChannelIdAndIsActiveTrue(channelId)).thenReturn(Optional.empty());
        when(botClient.testConnection(channelId, guildId)).thenReturn(new TestConnectionResponse(true, null));
        when(botClient.createChannelInvite(channelId)).thenReturn(new CreateInviteResponse(false, null, null));
        when(linkedChannelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.connectChannel(projectId, channelId, guildId);

        assertThat(crossLink.isActive()).isFalse();
    }

    @Test
    void shouldCreateDefaultSettingsWhenMissing() {
        var channelId = "123456789";
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.empty());
        when(linkedChannelRepository.findByDiscordChannelIdAndIsActiveTrue(channelId)).thenReturn(Optional.empty());
        when(botClient.testConnection(channelId, null)).thenReturn(new TestConnectionResponse(true, null));
        when(botClient.createChannelInvite(channelId)).thenReturn(new CreateInviteResponse(false, null, null));

        var savedLink = LinkedChannelEntity.builder()
                .id(UUID.randomUUID())
                .project(project)
                .discordChannelId(channelId)
                .isActive(true)
                .build();
        when(linkedChannelRepository.save(any())).thenReturn(savedLink);
        when(settingsRepository.findByLinkedChannelId(savedLink.getId())).thenReturn(Optional.empty());

        service.connectChannel(projectId, channelId);

        verify(settingsRepository).save(any(DiscordChannelSettingsEntity.class));
    }

    // ── disconnectChannel ──

    @Test
    void shouldDisconnectChannel() {
        var link = LinkedChannelEntity.builder()
                .id(UUID.randomUUID())
                .project(project)
                .discordChannelId("ch-1")
                .discordInviteUrl("https://discord.gg/abc")
                .isActive(true)
                .build();
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.of(link));
        when(botClient.leaveGuild("ch-1")).thenReturn(new LeaveGuildResponse(true, null));
        when(linkedChannelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.disconnectChannel(projectId);

        assertThat(link.isActive()).isFalse();
        assertThat(link.getDiscordInviteUrl()).isNull();
        verify(botClient).leaveGuild("ch-1");
    }

    @Test
    void shouldDisconnectEvenWhenLeaveGuildFails() {
        var link = LinkedChannelEntity.builder()
                .id(UUID.randomUUID())
                .project(project)
                .discordChannelId("ch-1")
                .discordInviteUrl("https://discord.gg/abc")
                .isActive(true)
                .build();
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.of(link));
        when(botClient.leaveGuild("ch-1")).thenReturn(new LeaveGuildResponse(false, "bot offline"));
        when(linkedChannelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.disconnectChannel(projectId);

        assertThat(link.isActive()).isFalse();
        verify(botClient).leaveGuild("ch-1");
    }

    @Test
    void shouldThrowWhenDisconnectNoChannel() {
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.disconnectChannel(projectId))
                .isInstanceOf(DiscordConnectionFailedException.class)
                .hasMessageContaining("No Discord channel linked");
    }

    // ── getLinkedChannel ──

    @Test
    void shouldGetLinkedChannel() {
        var link = LinkedChannelEntity.builder().id(UUID.randomUUID()).project(project).build();
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.of(link));

        var result = service.getLinkedChannel(projectId);

        assertThat(result).isEqualTo(link);
    }

    @Test
    void shouldThrowWhenNoLinkedChannel() {
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLinkedChannel(projectId))
                .isInstanceOf(DiscordConnectionFailedException.class);
    }

    // ── updateDiscordInviteUrl ──

    @Test
    void shouldUpdateInviteUrl() {
        var link = LinkedChannelEntity.builder()
                .id(UUID.randomUUID()).project(project).build();
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.of(link));
        when(linkedChannelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateDiscordInviteUrl(projectId, "https://discord.gg/new");

        assertThat(result.getDiscordInviteUrl()).isEqualTo("https://discord.gg/new");
    }

    // ── getAvailableGuilds ──

    @Test
    void shouldReturnAvailableGuilds() {
        var g1 = new GuildInfo("g-1", "Guild One");
        var g2 = new GuildInfo("g-2", "Guild Two");
        var link = LinkedChannelEntity.builder()
                .id(UUID.randomUUID()).project(project).discordGuildId("g-1").isActive(true).build();

        when(botClient.getGuilds()).thenReturn(new GuildsResponse(List.of(g1, g2), true, null));
        when(linkedChannelRepository.findAll()).thenReturn(List.of(link));

        var result = service.getAvailableGuilds(projectId);

        assertThat(result).containsExactly(g2);
    }

    @Test
    void shouldThrowWhenGuildsResponseFails() {
        when(botClient.getGuilds()).thenReturn(new GuildsResponse(null, false, "error"));

        assertThatThrownBy(() -> service.getAvailableGuilds(projectId))
                .isInstanceOf(DiscordConnectionFailedException.class);
    }

    // ── autoConnectChannel ──

    @Test
    void shouldAutoConnectWithGuildId() {
        var guildId = "guild-1";
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(botClient.autoSetup(owner.getDiscordId(), guildId))
                .thenReturn(new AutoSetupResponse(true, guildId, "new-ch", "general", null, true));
        when(botClient.testConnection("new-ch", guildId))
                .thenReturn(new TestConnectionResponse(true, null));

        var link = LinkedChannelEntity.builder()
                .id(UUID.randomUUID())
                .project(project)
                .discordChannelId("new-ch")
                .isActive(true)
                .build();
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.empty());
        when(linkedChannelRepository.findByDiscordChannelIdAndIsActiveTrue("new-ch")).thenReturn(Optional.empty());
        when(botClient.createChannelInvite("new-ch")).thenReturn(new CreateInviteResponse(false, null, null));
        when(linkedChannelRepository.save(any())).thenReturn(link);
        when(settingsRepository.findByLinkedChannelId(any())).thenReturn(Optional.of(mock(DiscordChannelSettingsEntity.class)));

        var result = service.autoConnectChannel(projectId, guildId);

        assertThat(result.discordChannelId()).isEqualTo("new-ch");
        assertThat(result.warning()).isNull();
    }

    @Test
    void shouldAutoConnectAndConsumeBotGuild() {
        var guildId = "consumed-guild";
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(discordAuthService.consumeBotGuild(projectId)).thenReturn(guildId);
        when(botClient.autoSetup(owner.getDiscordId(), guildId))
                .thenReturn(new AutoSetupResponse(true, guildId, "new-ch", "general", null, false));
        when(botClient.testConnection("new-ch", guildId))
                .thenReturn(new TestConnectionResponse(true, null));

        var link = LinkedChannelEntity.builder()
                .id(UUID.randomUUID())
                .project(project)
                .discordChannelId("new-ch")
                .isActive(true)
                .build();
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.empty());
        when(linkedChannelRepository.findByDiscordChannelIdAndIsActiveTrue("new-ch")).thenReturn(Optional.empty());
        when(botClient.createChannelInvite("new-ch")).thenReturn(new CreateInviteResponse(false, null, null));
        when(linkedChannelRepository.save(any())).thenReturn(link);
        when(settingsRepository.findByLinkedChannelId(any())).thenReturn(Optional.of(mock(DiscordChannelSettingsEntity.class)));

        var result = service.autoConnectChannel(projectId);

        assertThat(result.warning()).isEqualTo("PROJECTSETTINGS.DISCORD.BOT_NO_WRITE_PERMISSION");
        verify(discordAuthService).consumeBotGuild(projectId);
    }

    @Test
    void shouldThrowWhenAutoSetupFails() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(botClient.autoSetup(owner.getDiscordId(), null))
                .thenReturn(new AutoSetupResponse(false, null, null, null, "no permission", true));

        assertThatThrownBy(() -> service.autoConnectChannel(projectId))
                .isInstanceOf(DiscordConnectionFailedException.class)
                .hasMessageContaining("auto-setup");
    }

    // ── getBotInviteUrl ──

    @Test
    void shouldGetBotInviteUrl() {
        var permissions = 76817;
        when(discordProperties.getBot()).thenReturn(mock(DiscordProperties.Bot.class));
        when(discordProperties.getBot().getInvitePermissions()).thenReturn(permissions);
        when(discordAuthService.createBotAuthUrl(projectId, userId, String.valueOf(permissions)))
                .thenReturn("https://discord.com/api/oauth2/authorize?...");

        var result = service.getBotInviteUrl(projectId, userId);

        assertThat(result).contains("discord.com");
        verify(discordAuthService).createBotAuthUrl(projectId, userId, String.valueOf(permissions));
    }

    // ── getSettings / updateSettings ──

    @Test
    void shouldGetSettings() {
        var linkId = UUID.randomUUID();
        var link = LinkedChannelEntity.builder().id(linkId).project(project).build();
        var settings = DiscordChannelSettingsEntity.builder().linkedChannel(link).build();
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.of(link));
        when(settingsRepository.findByLinkedChannelId(linkId)).thenReturn(Optional.of(settings));

        var result = service.getSettings(projectId);

        assertThat(result).isEqualTo(settings);
    }

    @Test
    void shouldUpdateSettings() {
        var linkId = UUID.randomUUID();
        var link = LinkedChannelEntity.builder().id(linkId).project(project).build();
        var existing = DiscordChannelSettingsEntity.builder()
                .linkedChannel(link).notifyPostCreated(true).notifyMemberJoin(true).build();
        var updated = DiscordChannelSettingsEntity.builder()
                .notifyPostCreated(false).notifyPostDeleted(true).build();
        when(linkedChannelRepository.findByProjectId(projectId)).thenReturn(Optional.of(link));
        when(settingsRepository.findByLinkedChannelId(linkId)).thenReturn(Optional.of(existing));
        when(settingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateSettings(projectId, updated);

        assertThat(result.isNotifyPostCreated()).isFalse();
        assertThat(result.isNotifyPostDeleted()).isTrue();
        assertThat(result.isNotifyMemberJoin()).isTrue();
    }
}
