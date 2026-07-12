import { Events, type Client, type DMChannel, type NonThreadGuildBasedChannel } from 'discord.js';
import { streamProducer } from '../../streams/producer.js';
import { logger } from '../../config/logger.js';
import { wrapAsync } from '../wrapAsync.js';

/** Registers the ChannelDelete handler — notifies the platform when our linked channel is removed. */
export function registerChannelDeleteHandler(client: Client): void {
  client.on(Events.ChannelDelete, wrapAsync(async (channel: DMChannel | NonThreadGuildBasedChannel) => {
    if (!channel.isTextBased()) return;
    if (channel.isDMBased()) return;

    await streamProducer.channelDisconnected({
      channelId: channel.id,
      reason: 'CHANNEL_DELETED',
    });

    logger.info({ channelId: channel.id, guildId: 'guild' in channel ? channel.guildId : undefined }, 'channel deleted, produced disconnection event');
  }, 'channelDelete'));
}
