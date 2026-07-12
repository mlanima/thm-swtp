import type { Router } from 'express';
import { discordClient } from '../../bot/client.js';
import { validateSecret } from '../middleware/auth.js';

/** Registers GET /internal/guilds — lists the guilds the bot has joined. */
export function registerGuildRoutes(router: Router): void {
  router.get('/internal/guilds', validateSecret, async (_req, res) => {
    const guilds = discordClient.guilds.cache.map(g => ({ id: g.id, name: g.name }));
    res.json({ guilds, success: true, reason: null });
  });
}
