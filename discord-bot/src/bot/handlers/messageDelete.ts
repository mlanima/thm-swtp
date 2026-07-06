import { Events, type Client, type Message, type PartialMessage } from 'discord.js';
import { streamProducer } from '../../streams/producer.js';
import { logger } from '../../config/logger.js';

export function registerMessageDeleteHandler(client: Client): void {
  client.on(Events.MessageDelete, async (message: Message | PartialMessage) => {
    if (message.author?.bot) return;
    if (!message.id) return;

    await streamProducer.discordMessageDeleted({
      discordMsgId: message.id,
    });

    logger.debug({ messageId: message.id }, 'discord message deleted event produced');
  });
}
