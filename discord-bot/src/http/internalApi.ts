import { PermissionFlagsBits } from 'discord.js';
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
  if (secret !== process.env.PLATFORM_API_SECRET) {
    res.status(401).json({ error: 'unauthorized' });
    return;
  }
  next();
}

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
    const channel = await discordClient.channels.fetch(channelId);
    if (!channel || !('guild' in channel) || !channel.guild) {
      res.json({ success: false, reason: 'channel not found or not in a guild' });
      return;
    }
    await channel.guild.leave();
    logger.info({ guildId: channel.guild.id, channelId }, 'bot left guild');
    res.json({ success: true });
  } catch (err: any) {
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
    const channel = await discordClient.channels.fetch(channelId);
    if (!channel || !channel.isTextBased() || !('guild' in channel) || !channel.guild) {
      res.json({ success: false, reason: 'channel not found or not a text channel in a guild' });
      return;
    }
    const guild = channel.guild;
    const botMember = guild.members.me;
    if (!botMember) {
      res.json({ success: false, reason: 'bot is not a member of this guild' });
      return;
    }
    // Set permission overwrites via direct REST calls for reliability
    const rest = (discordClient as any).rest;
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
    } catch (innerErr: any) {
      logger.warn({ err: innerErr, channelId }, 'member permission overwrites failed, everyone restrict still applied');
    }
    logger.info({ channelId, guildId: guild.id }, 'channel restricted to owner and bot');
    res.json({ success: true });
  } catch (err: any) {
    logger.error({ err, channelId }, 'restrict channel failed');
    if (err.code === 50013) {
      res.json({ success: false, reason: 'bot missing Manage Channels permission' });
    } else {
      res.json({ success: false, reason: 'failed to restrict channel' });
    }
  }
});

internalApi.post('/internal/channels/:channelId/invite', validateSecret, async (req, res) => {
  const channelId = String(req.params.channelId);
  try {
    const channel = await discordClient.channels.fetch(channelId);
    if (!channel || !channel.isTextBased() || !('guild' in channel) || !channel.guild) {
      res.json({ success: false, reason: 'channel not found, not a text channel, or not in a guild' });
      return;
    }
    const invite = await (channel as any).createInvite({ maxAge: 0, maxUses: 0 });
    logger.info({ channelId, code: invite.code }, 'discord invite created');
    res.json({ success: true, inviteUrl: `https://discord.gg/${invite.code}` });
  } catch (err: any) {
    logger.error({ err, channelId }, 'create-invite failed');
    if (err.code === 50013) {
      res.json({ success: false, reason: 'bot missing Create Invite permission' });
    } else {
      res.json({ success: false, reason: 'failed to create invite' });
    }
  }
});

internalApi.post('/internal/auto-setup', validateSecret, async (req, res) => {
  const { ownerDiscordId } = req.body as { ownerDiscordId?: string };
  try {
    const guild = discordClient.guilds.cache.first();
    if (!guild) {
      res.json({ success: false, reason: 'bot is not in any guild' });
      return;
    }
    const channelName = 'posts';
    const existing = guild.channels.cache.find(
      c => c.name === channelName && c.isTextBased()
    );
    if (existing) {
      logger.info({ guildId: guild.id, channelId: existing.id }, 'auto-setup: found existing #posts');
      res.json({ success: true, guildId: guild.id, channelId: existing.id, channelName: existing.name });
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
    res.json({ success: true, guildId: guild.id, channelId: created.id, channelName: created.name });
  } catch (err: any) {
    logger.error({ err }, 'auto-setup failed');
    if (err.code === 50013) {
      res.json({ success: false, reason: 'bot missing Manage Channels permission' });
    } else {
      res.json({ success: false, reason: 'failed to create or find channel' });
    }
  }
});

internalApi.post('/internal/test-connection', validateSecret, async (req, res) => {
  const { channelId } = req.body as { channelId?: string };
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

export function startInternalApi(port: number): void {
  internalApi.listen(port, () => {
    logger.info({ port }, 'internal API listening');
  });
}

