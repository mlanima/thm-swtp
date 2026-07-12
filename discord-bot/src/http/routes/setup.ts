import { PermissionFlagsBits, DiscordAPIError } from 'discord.js';
import type { Router } from 'express';
import { logger } from '../../config/logger.js';
import { discordClient } from '../../bot/client.js';
import { validateSecret } from '../middleware/auth.js';

/** Registers POST /internal/auto-setup — one-shot guild + channel setup for the platform. */
export function registerSetupRoutes(router: Router): void {
  router.post('/internal/auto-setup', validateSecret, async (req, res) => {
    const { ownerDiscordId, guildId } = req.body as { ownerDiscordId?: string; guildId?: string };

    try {
      let guild = null;
      if (guildId) {
        guild = discordClient.guilds.cache.get(guildId) ?? await discordClient.guilds.fetch(guildId).catch(() => null);
      } else {
        // No guildId given — try to auto-detect if the bot is in exactly one guild
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

      // Look for an existing #posts channel
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
      // Create #posts with restricted permissions (only owner + bot can write)
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
      const code = err instanceof DiscordAPIError ? err.code : undefined;
      if (code === 50013) {
        res.json({ success: false, reason: 'bot missing Manage Channels permission' });
      } else {
        res.json({ success: false, reason: 'failed to create or find channel' });
      }
    }
  });
}
