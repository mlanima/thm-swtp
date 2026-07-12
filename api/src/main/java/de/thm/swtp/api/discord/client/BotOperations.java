package de.thm.swtp.api.discord.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

public interface BotOperations {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GuildInfo(String id, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GuildsResponse(List<GuildInfo> guilds, boolean success, String reason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TestConnectionResponse(boolean success, String reason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RetryJobResponse(boolean success) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CreateInviteResponse(boolean success, String inviteUrl, String reason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AutoSetupResponse(boolean success, String guildId, String channelId, String channelName, String reason, boolean canWrite) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LeaveGuildResponse(boolean success, String reason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RestrictChannelResponse(boolean success, String reason) {}

    TestConnectionResponse testConnection(String channelId);

    TestConnectionResponse testConnection(String channelId, String guildId);

    RetryJobResponse retryJob(String jobId);

    CreateInviteResponse createChannelInvite(String channelId);

    LeaveGuildResponse leaveGuild(String channelId);

    RestrictChannelResponse restrictChannel(String channelId, String ownerDiscordId);

    AutoSetupResponse autoSetup(String ownerDiscordId);

    AutoSetupResponse autoSetup(String ownerDiscordId, String guildId);

    GuildsResponse getGuilds();
}
