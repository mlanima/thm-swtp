import { Events, type Client, type Message } from 'discord.js';
import { streamProducer } from '../../streams/producer.js';
import { redis } from '../../config/redis.js';
import { logger } from '../../config/logger.js';

const DEDUP_TTL = 86400;

export function registerMessageCreateHandler(client: Client): void {
  client.on(Events.MessageCreate, async (message: Message) => {
    if (message.author.bot) return;
    if (message.webhookId) return;

    if (!message.guild) return;

    const dedupKey = `dedup:discord:msg:${message.id}`;
    const exists = await redis.set(dedupKey, '1', 'EX', DEDUP_TTL, 'NX');
    if (exists !== 'OK') return;

    const attachmentUrls = message.attachments.map((a) => a.url);
    const attachmentList = attachmentUrls.length > 0 ? attachmentUrls : undefined;

    await streamProducer.discordMessageCreated({
      discordMsgId: message.id,
      channelId: message.channelId,
      content: message.content,
      discordUserId: message.author.id,
      discordUsername: message.author.username,
      attachmentUrls: attachmentList,
    });

    logger.debug({ messageId: message.id, channelId: message.channelId }, 'discord message created event produced');
  });
}
