import { Events, type Client, type Message, type PartialMessage } from 'discord.js';
import { streamProducer } from '../../streams/producer.js';
import { logger } from '../../config/logger.js';
import { wrapAsync } from '../wrapAsync.js';

export function registerMessageUpdateHandler(client: Client): void {
  client.on(Events.MessageUpdate, wrapAsync(async (_old: Message | PartialMessage, message: Message | PartialMessage) => {
    if (message.author?.bot) return;
    if (message.webhookId) return;
    if (!message.id) return;

    const full = message.partial ? await message.fetch() : message;
    if (full.author?.bot) return;

    await streamProducer.discordMessageUpdated({
      discordMsgId: full.id,
      content: full.content ?? '',
    });

    logger.debug({ messageId: full.id }, 'discord message updated event produced');
  }, 'messageUpdate'));
}
