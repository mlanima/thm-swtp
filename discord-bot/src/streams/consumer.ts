import { redis } from '../config/redis.js';
import { logger } from '../config/logger.js';
import { validateStreamMessage } from './validator.js';
import { discordQueue } from '../queues/discordQueue.js';
import { eventRegistry } from '../events/eventRegistry.js';
import type { Redis } from 'ioredis';
import type { Queue } from 'bullmq';
import type { Logger } from 'pino';

/** Dependencies for the Redis stream consumer factory. */
export interface ConsumerDeps {
  redis: Redis;
  logger: Logger;
  queue: Queue;
  registry: typeof eventRegistry;
  streamKey?: string;
  consumerGroup?: string;
  consumerName?: string;
  batchSize?: number;
  blockMs?: number;
  retryDelayMs?: number;
}

/** Factory that creates a Redis stream consumer with injected deps. Processes platform→bot events. */
export function createConsumer(deps: ConsumerDeps) {
  const {
    redis,
    logger,
    queue,
    registry,
    streamKey = 'stream:discord:sync',
    consumerGroup = 'discord-bot',
    consumerName = `worker-${process.pid}`,
    batchSize = 10,
    blockMs = 5000,
    retryDelayMs = 1000,
  } = deps;

  /** Creates the consumer group if it doesn't exist yet (idempotent). */
  async function ensureGroup(): Promise<void> {
    try {
      // $ = start from now (no old messages), MKSTREAM = auto-create the stream key
      await redis.xgroup('CREATE', streamKey, consumerGroup, '$', 'MKSTREAM');
      logger.info({ group: consumerGroup }, 'created consumer group');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      // BUSYGROUP means the group already exists — that's fine
      if (!msg.includes('BUSYGROUP')) {
        logger.error({ err }, 'failed to create consumer group');
        throw err;
      }
    }
  }

  /**
   * Reads one batch of pending messages, validates each, and enqueues them as BullMQ jobs.
   * Messages that fail validation are acknowledged (acked) and skipped.
   */
  async function processBatch(): Promise<void> {
    try {
      // BLOCK = wait up to blockMs for new messages, > = unclaimed messages only
      const raw = await redis.xreadgroup(
        'GROUP', consumerGroup, consumerName,
        'COUNT', batchSize,
        'BLOCK', blockMs,
        'STREAMS', streamKey, '>',
      );

      if (!raw) return;

      // Raw shape: [[streamKey, [[messageId, [field, value, field, value, ...]], ...]], ...]
      const results = raw as Array<[string, Array<[string, string[]]>]>;
      for (const streamEntry of results) {
        const messages = streamEntry[1];
        for (const [messageId, fields] of messages) {
          // Flatten [key, val, key, val, ...] into a map
          const fieldMap: Record<string, string> = {};
          for (let i = 0; i < fields.length; i += 2) {
            fieldMap[fields[i]] = fields[i + 1];
          }

          try {
            const parsed = validateStreamMessage({
              type: fieldMap.type,
              payload: JSON.parse(fieldMap.payload),
            });

            const config = registry[parsed.type as keyof typeof registry];
            if (!config) {
              logger.warn({ type: parsed.type }, 'unknown event type');
              await redis.xack(streamKey, consumerGroup, messageId);
              continue;
            }

            // Push to BullMQ with the retry config from the registry
            await queue.add(config.jobName, parsed.payload, {
              attempts: config.defaultAttempts,
              backoff: { type: 'exponential', delay: 15_000 },
            });

            await redis.xack(streamKey, consumerGroup, messageId);
            logger.debug({ type: parsed.type, messageId }, 'enqueued job');
          } catch (err) {
            // ZodErrors mean the payload is malformed — ack and skip to avoid blocking the stream
            if (err instanceof Error && err.name === 'ZodError') {
              logger.warn({ err, messageId }, 'invalid stream payload, acking without job');
              await redis.xack(streamKey, consumerGroup, messageId);
            } else {
              logger.error({ err, messageId }, 'failed to process stream message');
            }
          }
        }
      }
    } catch (err) {
      logger.error({ err }, 'stream consumer error');

      // Consumer group was probably deleted — recreate it
      if (err instanceof Error && err.message.includes('NOGROUP')) {
        try {
          await ensureGroup();
        } catch {
          logger.error({ err }, 'failed to recreate consumer group');
        }
      }

      // Wait before retrying to avoid a tight loop on persistent errors
      await new Promise(resolve => setTimeout(resolve, retryDelayMs));
    }
  }

  return { processBatch, ensureGroup };
}

/** Default consumer wired to the real Redis, queue, and registry. */
const defaultConsumer = createConsumer({ redis, logger, queue: discordQueue, registry: eventRegistry });

/** Ensures the consumer group exists, then starts the infinite read→process→ack loop. */
export async function startStreamConsumer(): Promise<void> {
  await defaultConsumer.ensureGroup();
  logger.info({ consumer: `worker-${process.pid}` }, 'starting stream consumer loop');

  setImmediate(function loop() {
    defaultConsumer.processBatch().finally(() => setImmediate(loop));
  });
}
