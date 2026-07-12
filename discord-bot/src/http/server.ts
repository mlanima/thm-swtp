import express from 'express';
import { logger } from '../config/logger.js';
import { registerHealthRoutes } from './routes/health.js';
import { registerMetricsRoutes } from './routes/metrics.js';
import { registerGuildRoutes } from './routes/guilds.js';
import { registerChannelRoutes } from './routes/channels.js';
import { registerSetupRoutes } from './routes/setup.js';
import { registerJobRoutes } from './routes/jobs.js';

/** Express app — assembles all route modules. */
export const app = express();
app.use(express.json());

registerHealthRoutes(app);
registerMetricsRoutes(app);
registerGuildRoutes(app);
registerChannelRoutes(app);
registerSetupRoutes(app);
registerJobRoutes(app);

let server: ReturnType<typeof app.listen> | null = null;

/** Starts the Express server on the given port. */
export function startServer(port: number): ReturnType<typeof app.listen> {
  server = app.listen(port, () => {
    logger.info({ port }, 'internal API listening');
  });
  return server;
}

/** Gracefully stops the Express server. */
export async function stopServer(): Promise<void> {
  return new Promise((resolve) => {
    if (server) {
      server.close(() => resolve());
      server = null;
    } else {
      resolve();
    }
  });
}
