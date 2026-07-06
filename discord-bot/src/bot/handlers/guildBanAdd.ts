import { Events, type Client, type GuildBan } from 'discord.js';
import { streamProducer } from '../../streams/producer.js';
import { logger } from '../../config/logger.js';

export function registerGuildBanAddHandler(client: Client): void {
  client.on(Events.GuildBanAdd, async (ban: GuildBan) => {
    if (ban.client.user && ban.user.id === ban.client.user.id) {
      const channels = ban.guild.channels.cache.filter((c) => c.isTextBased());
      for (const [, channel] of channels) {
        await streamProducer.channelDisconnected({
          channelId: channel.id,
          reason: 'BOT_BANNED',
        });
      }
      logger.warn({ guildId: ban.guild.id }, 'bot was banned from guild, disconnected all linked channels');
    }
  });
}
