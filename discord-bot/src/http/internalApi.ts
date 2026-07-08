import { PermissionFlagsBits, type TextChannel, type NewsChannel, type GuildBasedChannel, type NonThreadGuildBasedChannel } from 'discord.js';
import express from 'express';
import { logger } from '../config/logger.js';
import { pingRedis } from '../config/redis.js';
import { discordClient, getDiscordStatus } from '../bot/client.js';
import { metricsRegistry } from '../metrics/index.js';
import { discordQueue } from '../queues/discordQueue.js';

export const internalApi = express();
internalApi.use(express.json());

function validateSecret(req: express.Request, res: express.Response, next: express.NextFunction) {
  const secret = req.headers['x-internal-secret'] as string | undefined;
  const expected = process.env.PLATFORM_API_SECRET;
  if (!secret || !expected || secret !== expected) {
    res.status(401).json({ error: 'unauthorized' });
    return;
  }
  next();
}

async function resolveGuildChannel(channelId: string): Promise<{
  channel: GuildBasedChannel & NonThreadGuildBasedChannel;
  error: null;
} | {
  channel: null;
  error: { status: number; json: Record<string, unknown> };
}> {
  const channel = await discordClient.channels.fetch(channelId);
  if (!channel || !channel.isTextBased() || !('guild' in channel) || !channel.guild) {
    return {
      channel: null,
      error: { status: 200, json: { success: false, reason: 'channel not found or not a text channel in a guild' } },
    };
  }
  return { channel: channel as GuildBasedChannel & NonThreadGuildBasedChannel, error: null };
}

internalApi.get('/internal/guilds', validateSecret, async (_req, res) => {
  const guilds = discordClient.guilds.cache.map(g => ({ id: g.id, name: g.name }));
  res.json({ guilds, success: true, reason: null });
});

internalApi.get('/health', async (_req, res) => {
  const redis = await pingRedis();
  const discord = getDiscordStatus();
  const status = redis && discord === 'ready' ? 'ok' : 'degraded';
  res.json({ status, discord, redis: redis ? 'up' : 'down' });
});

internalApi.get('/metrics', async (_req, res) => {
  res.set('Content-Type', metricsRegistry.contentType);
  res.send(await metricsRegistry.metrics());
});

internalApi.post('/internal/channels/:channelId/leave-guild', validateSecret, async (req, res) => {
  const channelId = String(req.params.channelId);
  try {
    const { channel, error } = await resolveGuildChannel(channelId);
    if (error) {
      res.json(error.json);
      return;
    }
    await channel.guild.leave();
    logger.info({ guildId: channel.guild.id, channelId }, 'bot left guild');
    res.json({ success: true });
  } catch (err: unknown) {
    logger.error({ err, channelId }, 'leave-guild failed');
    res.json({ success: false, reason: 'failed to leave guild' });
  }
});

internalApi.post('/internal/channels/:channelId/restrict', validateSecret, async (req, res) => {
  const channelId = String(req.params.channelId);
  const { ownerDiscordId } = req.body as { ownerDiscordId?: string };
  if (!ownerDiscordId) {
    res.status(400).json({ success: false, reason: 'ownerDiscordId is required' });
    return;
  }
  try {
    const { channel, error } = await resolveGuildChannel(channelId);
    if (error) {
      res.json(error.json);
      return;
    }
    const guild = channel.guild;
    const botMember = guild.members.me;
    if (!botMember) {
      res.json({ success: false, reason: 'bot is not a member of this guild' });
      return;
    }
    const rest = discordClient.rest;
    const sm = String(PermissionFlagsBits.SendMessages);
    await rest.put(`/channels/${channelId}/permissions/${guild.roles.everyone.id}`, {
      body: { type: 0, allow: '0', deny: sm },
    });
    try {
      await rest.put(`/channels/${channelId}/permissions/${ownerDiscordId}`, {
        body: { type: 1, allow: sm, deny: '0' },
      });
      await rest.put(`/channels/${channelId}/permissions/${botMember.id}`, {
        body: { type: 1, allow: sm, deny: '0' },
      });
    } catch (innerErr: unknown) {
      const msg = innerErr instanceof Error ? innerErr : new Error(String(innerErr));
      logger.warn({ err: msg, channelId }, 'member permission overwrites failed, everyone restrict still applied');
    }
    logger.info({ channelId, guildId: guild.id }, 'channel restricted to owner and bot');
    res.json({ success: true });
  } catch (err: unknown) {
    logger.error({ err, channelId }, 'restrict channel failed');
    const code = err instanceof Error ? (err as unknown as { code: unknown }).code : undefined;
    if (code === 50013) {
      res.json({ success: false, reason: 'bot missing Manage Channels permission' });
    } else {
      res.json({ success: false, reason: 'failed to restrict channel' });
    }
  }
});

internalApi.post('/internal/channels/:channelId/invite', validateSecret, async (req, res) => {
  const channelId = String(req.params.channelId);
  try {
    const { channel, error } = await resolveGuildChannel(channelId);
    if (error) {
      res.json(error.json);
      return;
    }
    if (!('createInvite' in channel)) {
      res.json({ success: false, reason: 'channel type does not support invites' });
      return;
    }
    const invite = await (channel as TextChannel | NewsChannel).createInvite({ maxAge: 0, maxUses: 0 });
    logger.info({ channelId, code: invite.code }, 'discord invite created');
    res.json({ success: true, inviteUrl: `https://discord.gg/${invite.code}` });
  } catch (err: unknown) {
    logger.error({ err, channelId }, 'create-invite failed');
    const code = err instanceof Error ? (err as unknown as { code: unknown }).code : undefined;
    if (code === 50013) {
      res.json({ success: false, reason: 'bot missing Create Invite permission' });
    } else {
      res.json({ success: false, reason: 'failed to create invite' });
    }
  }
});

internalApi.post('/internal/auto-setup', validateSecret, async (req, res) => {
  const { ownerDiscordId, guildId } = req.body as { ownerDiscordId?: string; guildId?: string };

  try {
    let guild = null;
    if (guildId) {
      guild = discordClient.guilds.cache.get(guildId) ?? await discordClient.guilds.fetch(guildId).catch(() => null);
    } else {
      const allGuilds = discordClient.guilds.cache;
      if (allGuilds.size === 0) {
        res.json({ success: false, reason: 'bot is not in any guild' });
        return;
      }
      if (allGuilds.size > 1) {
        res.json({ success: false, reason: 'bot is in multiple guilds — specify guildId' });
        return;
      }
      guild = allGuilds.first();
    }

    if (!guild) {
      res.json({ success: false, reason: 'guild not found' });
      return;
    }

    const channelName = 'posts';
    const existing = guild.channels.cache.find(
      c => c.name === channelName && c.isTextBased()
    );
    if (existing) {
      const botMember = guild.members.me;
      const canWrite = botMember
        ? existing.permissionsFor(botMember)?.has(PermissionFlagsBits.SendMessages) ?? false
        : false;
      logger.info({ guildId: guild.id, channelId: existing.id, canWrite }, 'auto-setup: found existing #posts');
      res.json({ success: true, guildId: guild.id, channelId: existing.id, channelName: existing.name, canWrite });
      return;
    }
    if (!ownerDiscordId) {
      res.json({ success: false, reason: 'ownerDiscordId is required to create channel' });
      return;
    }
    const botMember = guild.members.me;
    if (!botMember) {
      res.json({ success: false, reason: 'bot is not a member of this guild' });
      return;
    }
    const sm = PermissionFlagsBits.SendMessages;
    const created = await guild.channels.create({
      name: channelName,
      reason: 'Auto-created by IdeaCamp bot',
      permissionOverwrites: [
        { id: guild.roles.everyone.id, deny: [sm] },
        { id: ownerDiscordId, allow: [sm], type: 1 },
        { id: botMember.id, allow: [sm], type: 1 },
      ],
    });
    logger.info({ guildId: guild.id, channelId: created.id }, 'auto-setup: created restricted #posts');
    res.json({ success: true, guildId: guild.id, channelId: created.id, channelName: created.name, canWrite: true });
  } catch (err: unknown) {
    logger.error({ err }, 'auto-setup failed');
    const code = err instanceof Error ? (err as unknown as { code: unknown }).code : undefined;
    if (code === 50013) {
      res.json({ success: false, reason: 'bot missing Manage Channels permission' });
    } else {
      res.json({ success: false, reason: 'failed to create or find channel' });
    }
  }
});

internalApi.post('/internal/test-connection', validateSecret, async (req, res) => {
  const { channelId, guildId } = req.body as { channelId?: string; guildId?: string };
  if (!channelId) {
    res.status(400).json({ success: false, reason: 'channelId is required' });
    return;
  }
  try {
    const channel = await discordClient.channels.fetch(channelId);
    if (!channel?.isTextBased()) {
      res.json({ success: false, reason: 'channel not found or not a text channel' });
      return;
    }
    if (guildId && 'guildId' in channel && channel.guildId !== guildId) {
      res.json({ success: false, reason: 'channel does not belong to the specified guild' });
      return;
    }
    res.json({ success: true });
  } catch (err) {
    logger.error({ err, channelId }, 'test-connection failed');
    res.json({ success: false, reason: 'bot cannot access this channel' });
  }
});

internalApi.post('/internal/jobs/:jobId/retry', validateSecret, async (req, res) => {
  const jobId = req.params.jobId as string;
  if (!jobId) {
    res.status(400).json({ success: false, reason: 'jobId is required' });
    return;
  }
  try {
    const job = await discordQueue.getJob(jobId);
    if (!job) {
      res.status(404).json({ success: false, reason: 'job not found' });
      return;
    }
    await job.retry();
    res.json({ success: true });
  } catch (err) {
    logger.error({ err, jobId }, 'retry failed');
    res.json({ success: false, reason: 'retry failed' });
  }
});

let server: ReturnType<typeof internalApi.listen> | null = null;

export function startInternalApi(port: number): ReturnType<typeof internalApi.listen> {
  server = internalApi.listen(port, () => {
    logger.info({ port }, 'internal API listening');
  });
  return server;
}

export async function stopInternalApi(): Promise<void> {
  return new Promise((resolve) => {
    if (server) {
      server.close(() => resolve());
      server = null;
    } else {
      resolve();
    }
  });
}
