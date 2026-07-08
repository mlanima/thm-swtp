import 'dotenv/config';
import { logger } from './config/logger.js';
import { discordClient, startDiscordClient } from './bot/client.js';
import { startStreamConsumer } from './streams/consumer.js';
import { startDiscordWorker, stopDiscordWorker } from './queues/discordQueue.js';
import { startInternalApi, stopInternalApi } from './http/internalApi.js';
import { redis } from './config/redis.js';

const PORT = Number(process.env.PORT ?? 3001);

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

async function main(): Promise<void> {
  logger.info('starting discord bot');

  process.on('SIGTERM', () => shutdown().finally(() => process.exit(0)));
  process.on('SIGINT', () => shutdown().finally(() => process.exit(0)));

  await startDiscordClient();
  logger.info('discord client logged in');

  startStreamConsumer().catch((err) => logger.error({ err }, 'failed to start stream consumer'));
  startDiscordWorker();
  startInternalApi(PORT);
}

main().catch((err) => {
  logger.fatal({ err }, 'failed to start bot');
  process.exit(1);
});
