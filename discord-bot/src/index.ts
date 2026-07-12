import 'dotenv/config';
import { logger } from './config/logger.js';
import { discordClient, startDiscordClient } from './bot/client.js';
import { startStreamConsumer } from './streams/consumer.js';
import { startDiscordWorker, stopDiscordWorker } from './queues/discordQueue.js';
import { startInternalApi, stopInternalApi } from './http/internalApi.js';
import { redis } from './config/redis.js';

const PORT = Number(process.env.PORT ?? 3001);

const REQUIRED_ENV_VARS = ['DISCORD_TOKEN', 'PLATFORM_API_SECRET'];

/** Exits immediately if any required env vars are missing. */
function validateEnv(): void {
  const missing = REQUIRED_ENV_VARS.filter((key) => !process.env[key]);
  if (missing.length > 0) {
    logger.fatal({ missing }, 'required environment variables not set');
    process.exit(1);
  }
}

/** Graceful shutdown — stops the API, destroys the Discord client, closes the worker, disconnects Redis. */
async function shutdown(): Promise<void> {
  logger.info('shutting down');

  await stopInternalApi();
  logger.info('internal API server closed');

  discordClient.destroy();
  logger.info('discord client destroyed');

  await stopDiscordWorker();
  logger.info('bullmq worker closed');

  redis.disconnect();
  logger.info('redis disconnected');
}

/** Bot entry point — validates env, starts all subsystems, registers shutdown handlers. */
async function main(): Promise<void> {
  validateEnv();
  logger.info('starting discord bot');

  process.on('SIGTERM', () => shutdown().finally(() => process.exit(0)));
  process.on('SIGINT', () => shutdown().finally(() => process.exit(0)));

  await startDiscordClient();
  logger.info('discord client logged in');

  // Consumer runs on a setImmediate loop, so we don't await it — errors are logged internally
  startStreamConsumer().catch((err) => logger.error({ err }, 'failed to start stream consumer'));

  try {
    startDiscordWorker();
  } catch (err) {
    logger.fatal({ err }, 'failed to start discord worker');
    process.exit(1);
  }

  try {
    startInternalApi(PORT);
  } catch (err) {
    logger.fatal({ err }, 'failed to start internal API');
    process.exit(1);
  }
}

main().catch((err) => {
  logger.fatal({ err }, 'failed to start bot');
  process.exit(1);
});
