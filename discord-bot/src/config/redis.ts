import { Redis } from 'ioredis';
import { logger } from './logger.js';

const redisHost = process.env.REDIS_HOST ?? 'localhost';
const redisPort = Number(process.env.REDIS_PORT ?? 6379);

export const redis = new Redis({
  host: redisHost,
  port: redisPort,
  maxRetriesPerRequest: null,
  retryStrategy: (times: number) => Math.min(times * 200, 5000),
});

redis.on('connect', () => logger.info({ host: redisHost, port: redisPort }, 'redis connected'));
redis.on('error', (err: Error) => logger.error({ err }, 'redis error'));

export async function pingRedis(): Promise<boolean> {
  try {
    const result = await redis.ping();
    return result === 'PONG';
  } catch {
    return false;
  }
}
