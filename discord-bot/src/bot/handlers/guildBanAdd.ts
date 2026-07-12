import { Events, type Client, type GuildBan } from 'discord.js';
import { streamProducer } from '../../streams/producer.js';
import { logger } from '../../config/logger.js';
import { wrapAsync } from '../wrapAsync.js';

/** Registers the GuildBanAdd handler — only reacts when the bot itself was banned. */
export function registerGuildBanAddHandler(client: Client): void {
  client.on(Events.GuildBanAdd, wrapAsync(async (ban: GuildBan) => {
    // Only process if the banned user is the bot itself
    if (ban.client.user && ban.user.id === ban.client.user.id) {
      // Disconnect every text channel in the guild we were banned from
      const channels = ban.guild.channels.cache.filter((c) => c.isTextBased());
      for (const [, channel] of channels) {
        await streamProducer.channelDisconnected({
          channelId: channel.id,
          reason: 'BOT_BANNED',
        });
      }
      logger.warn({ guildId: ban.guild.id }, 'bot was banned from guild, disconnected all linked channels');
    }
  }, 'guildBanAdd'));
}
