import { Client, GatewayIntentBits } from 'discord.js';
import { logger } from '../config/logger.js';
export const discordClient = new Client({
    intents: [
        GatewayIntentBits.Guilds,
        GatewayIntentBits.GuildMessages,
        GatewayIntentBits.MessageContent,
        GatewayIntentBits.DirectMessages,
    ],
});
discordClient.once('ready', () => {
    logger.info({ user: discordClient.user?.tag }, 'discord client ready');
});
discordClient.on('error', (err) => {
    logger.error({ err }, 'discord client error');
});
export async function startDiscordClient() {
    const token = process.env.DISCORD_TOKEN;
    if (!token) {
        throw new Error('DISCORD_TOKEN is not set');
    }
    await discordClient.login(token);
}
export function getDiscordStatus() {
    switch (discordClient.ws.status) {
        case 0:
            return 'ready';
        case 1:
        case 2:
            return 'reconnecting';
        default:
            return 'disconnected';
    }
}
//# sourceMappingURL=client.js.map