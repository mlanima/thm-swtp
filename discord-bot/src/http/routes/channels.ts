import { PermissionFlagsBits, DiscordAPIError, type TextChannel, type NewsChannel } from 'discord.js';
import type { Router } from 'express';
import { logger } from '../../config/logger.js';
import { discordClient } from '../../bot/client.js';
import { resolveGuildChannel } from '../../bot/services/channelService.js';
import { validateSecret } from '../middleware/auth.js';

/** Registers channel-management endpoints (leave, restrict, invite, test). */
export function registerChannelRoutes(router: Router): void {
  /** Makes the bot leave the guild that owns a given channel. */
  router.post('/internal/channels/:channelId/leave-guild', validateSecret, async (req, res) => {
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

  /**
   * Restricts a channel so only the project owner and the bot can send messages.
   * Denies SendMessages for @everyone, then grants it to the owner and bot.
   */
  router.post('/internal/channels/:channelId/restrict', validateSecret, async (req, res) => {
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
      // Deny everyone first
      await rest.put(`/channels/${channelId}/permissions/${guild.roles.everyone.id}`, {
        body: { type: 0, allow: '0', deny: sm },
      });
      try {
        // Then allow the owner and bot
        await rest.put(`/channels/${channelId}/permissions/${ownerDiscordId}`, {
          body: { type: 1, allow: sm, deny: '0' },
        });
        await rest.put(`/channels/${channelId}/permissions/${botMember.id}`, {
          body: { type: 1, allow: sm, deny: '0' },
        });
      } catch (innerErr: unknown) {
        // Member overwrites can fail if the user left the guild — that's ok, everyone restrict still works
        const msg = innerErr instanceof Error ? innerErr : new Error(String(innerErr));
        logger.warn({ err: msg, channelId }, 'member permission overwrites failed, everyone restrict still applied');
      }
      logger.info({ channelId, guildId: guild.id }, 'channel restricted to owner and bot');
      res.json({ success: true });
    } catch (err: unknown) {
      logger.error({ err, channelId }, 'restrict channel failed');
      const code = err instanceof DiscordAPIError ? err.code : undefined;
      if (code === 50013) {
        res.json({ success: false, reason: 'bot missing Manage Channels permission' });
      } else {
        res.json({ success: false, reason: 'failed to restrict channel' });
      }
    }
  });

  /** Creates a never-expiring, unlimited-use Discord invite for a channel. */
  router.post('/internal/channels/:channelId/invite', validateSecret, async (req, res) => {
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
      const code = err instanceof DiscordAPIError ? err.code : undefined;
      if (code === 50013) {
        res.json({ success: false, reason: 'bot missing Create Invite permission' });
      } else {
        res.json({ success: false, reason: 'failed to create invite' });
      }
    }
  });

  /** Tests whether the bot can access a given channel. */
  router.post('/internal/test-connection', validateSecret, async (req, res) => {
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
}
