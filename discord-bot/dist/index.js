import { logger } from './config/logger.js';
import { startDiscordClient } from './bot/client.js';
import { startStreamConsumer } from './streams/consumer.js';
import { startDiscordWorker } from './queues/discordQueue.js';
import { startInternalApi } from './http/internalApi.js';
const PORT = Number(process.env.PORT ?? 3001);
async function main() {
    logger.info('starting discord bot');
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
//# sourceMappingURL=index.js.map