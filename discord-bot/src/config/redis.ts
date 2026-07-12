import { Redis } from 'ioredis';
import { logger } from './logger.js';

const redisHost = process.env.REDIS_HOST ?? 'localhost';
const redisPort = Number(process.env.REDIS_PORT ?? 6379);

/** Singleton Redis client used by streams, queues, and the dedup check. */
export const redis = new Redis({
  host: redisHost,
  port: redisPort,
  // null = BullMQ manages retries itself so we don't interfere
  maxRetriesPerRequest: null,
  retryStrategy: (times: number) => Math.min(times * 200, 5000),
});

redis.on('connect', () => logger.info({ host: redisHost, port: redisPort }, 'redis connected'));
redis.on('error', (err: Error) => logger.error({ err }, 'redis error'));

/** Typed connection config for BullMQ — uses the same Redis host/port (avoids `as any` cast). */
export const redisConnection = {
  host: redisHost,
  port: redisPort,
};

/** Quick health check — returns true if Redis responds to PING. */
export async function pingRedis(): Promise<boolean> {
  try {
    const result = await redis.ping();
    return result === 'PONG';
  } catch {
    return false;
  }
}
