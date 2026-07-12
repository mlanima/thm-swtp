package de.thm.swtp.api.discord.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Contract for communicating with the internal Discord bot service.
 * Each method maps to a bot HTTP endpoint for guild/channel management.
 */
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

    /**
     * Checks whether the bot can reach the given channel.
     * Guild ID is optional — without it the bot figures it out on its own.
     */
    TestConnectionResponse testConnection(String channelId);

    TestConnectionResponse testConnection(String channelId, String guildId);

    RetryJobResponse retryJob(String jobId);

    CreateInviteResponse createChannelInvite(String channelId);

    LeaveGuildResponse leaveGuild(String channelId);

    RestrictChannelResponse restrictChannel(String channelId, String ownerDiscordId);

    AutoSetupResponse autoSetup(String ownerDiscordId);

    AutoSetupResponse autoSetup(String ownerDiscordId, String guildId);

    /**
     * Lists all Discord servers (guilds) the bot is currently invited to.
     */
    GuildsResponse getGuilds();
}
