import { Events, type Client, type Message, type PartialMessage } from 'discord.js';
import { streamProducer } from '../../streams/producer.js';
import { logger } from '../../config/logger.js';
import { wrapAsync } from '../wrapAsync.js';

export function registerMessageDeleteHandler(client: Client): void {
  client.on(Events.MessageDelete, wrapAsync(async (message: Message | PartialMessage) => {
    if (message.author?.bot) return;
    if (!message.id) return;

    const full = message.partial ? await message.fetch() : message;
    if (full.author?.bot) return;

    await streamProducer.discordMessageDeleted({
      discordMsgId: full.id,
    });

    logger.debug({ messageId: full.id }, 'discord message deleted event produced');
  }, 'messageDelete'));
}
