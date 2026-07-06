import { Events, type Client, type DMChannel, type NonThreadGuildBasedChannel } from 'discord.js';
import { streamProducer } from '../../streams/producer.js';
import { logger } from '../../config/logger.js';

export function registerChannelDeleteHandler(client: Client): void {
  client.on(Events.ChannelDelete, async (channel: DMChannel | NonThreadGuildBasedChannel) => {
    if (!channel.isTextBased()) return;
    if (channel.isDMBased()) return;

    await streamProducer.channelDisconnected({
      channelId: channel.id,
      reason: 'CHANNEL_DELETED',
    });

    logger.info({ channelId: channel.id, guildId: 'guild' in channel ? channel.guildId : undefined }, 'channel deleted, produced disconnection event');
  });
}
