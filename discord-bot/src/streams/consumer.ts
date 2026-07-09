import { redis } from '../config/redis.js';
import { logger } from '../config/logger.js';
import { validateStreamMessage } from './validator.js';
import { discordQueue } from '../queues/discordQueue.js';

const OUTBOUND_STREAM = 'stream:discord:sync';
const CONSUMER_GROUP = 'discord-bot';
const CONSUMER_NAME = `worker-${process.pid}`;
const BATCH_SIZE = 10;
const BLOCK_MS = 5000;

async function ensureGroup(): Promise<void> {
  try {
    await redis.xgroup('CREATE', OUTBOUND_STREAM, CONSUMER_GROUP, '$', 'MKSTREAM');
    logger.info({ group: CONSUMER_GROUP }, 'created consumer group');
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : String(err);
    if (!msg.includes('BUSYGROUP')) {
      logger.error({ err }, 'failed to create consumer group');
      throw err;
    }
  }
}

const RETRY_DELAY_MS = 1000;

const eventToJob: Record<string, string> = {
  POST_CREATED: 'sendPost',
  POST_UPDATED: 'editPost',
  POST_DELETED: 'deletePost',
  PROJECT_INVITE: 'sendInvite',
  PROJECT_EVENT: 'sendEvent',
};

const jobAttempts: Record<string, number> = {
  sendPost: 5,
  editPost: 3,
  deletePost: 3,
  sendInvite: 3,
  sendEvent: 3,
};

async function processBatch(): Promise<void> {
  try {
    const raw = await redis.xreadgroup(
      'GROUP', CONSUMER_GROUP, CONSUMER_NAME,
      'COUNT', BATCH_SIZE,
      'BLOCK', BLOCK_MS,
      'STREAMS', OUTBOUND_STREAM, '>',
    );

    if (!raw) return;

    const results = raw as [[string, [[string, string[]]]]];
    for (const streamEntry of results) {
      const messages = streamEntry[1];
      for (const [messageId, fields] of messages) {
        const fieldMap: Record<string, string> = {};
        for (let i = 0; i < fields.length; i += 2) {
          fieldMap[fields[i]] = fields[i + 1];
        }

        try {
          const parsed = validateStreamMessage({
            type: fieldMap.type,
            payload: JSON.parse(fieldMap.payload),
          });

          const jobName = eventToJob[parsed.type];
          if (!jobName) {
            logger.warn({ type: parsed.type }, 'unknown event type');
            await redis.xack(OUTBOUND_STREAM, CONSUMER_GROUP, messageId);
            continue;
          }

          await discordQueue.add(jobName, parsed.payload, {
            attempts: jobAttempts[jobName] ?? 3,
            backoff: { type: 'exponential', delay: 15_000 },
          });

          await redis.xack(OUTBOUND_STREAM, CONSUMER_GROUP, messageId);
          logger.debug({ type: parsed.type, messageId }, 'enqueued job');
        } catch (err) {
          logger.warn({ err, messageId }, 'invalid stream message, acking without job');
          await redis.xack(OUTBOUND_STREAM, CONSUMER_GROUP, messageId);
        }
      }
    }
  } catch (err) {
    logger.error({ err }, 'stream consumer error');

    if (err instanceof Error && err.message.includes('NOGROUP')) {
      try {
        await ensureGroup();
      } catch {
        logger.error({ err }, 'failed to recreate consumer group');
      }
    }

    await new Promise(resolve => setTimeout(resolve, RETRY_DELAY_MS));
  }
}

export async function startStreamConsumer(): Promise<void> {
  await ensureGroup();
  logger.info({ consumer: CONSUMER_NAME }, 'starting stream consumer loop');

  setImmediate(function loop() {
    processBatch().finally(() => setImmediate(loop));
  });
}
