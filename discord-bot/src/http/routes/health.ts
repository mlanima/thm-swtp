import type { Router } from 'express';
import { pingRedis } from '../../config/redis.js';
import { getDiscordStatus } from '../../bot/client.js';

/** Registers GET /health — returns overall bot status (ok / degraded). */
export function registerHealthRoutes(router: Router): void {
  router.get('/health', async (_req, res) => {
    const redis = await pingRedis();
    const discord = getDiscordStatus();
    const status = redis && discord === 'ready' ? 'ok' : 'degraded';
    res.json({ status, discord, redis: redis ? 'up' : 'down' });
  });
}
