import { Client, GatewayIntentBits } from 'discord.js';
import { logger } from '../config/logger.js';
import { registerMessageCreateHandler } from './handlers/messageCreate.js';
import { registerMessageUpdateHandler } from './handlers/messageUpdate.js';
import { registerMessageDeleteHandler } from './handlers/messageDelete.js';
import { registerInteractionCreateHandler } from './handlers/interactionCreate.js';
import { registerChannelDeleteHandler } from './handlers/channelDelete.js';
import { registerGuildBanAddHandler } from './handlers/guildBanAdd.js';

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

export async function startDiscordClient(): Promise<void> {
  const token = process.env.DISCORD_TOKEN;
  if (!token) {
    throw new Error('DISCORD_TOKEN is not set');
  }

  registerMessageCreateHandler(discordClient);
  registerMessageUpdateHandler(discordClient);
  registerMessageDeleteHandler(discordClient);
  registerInteractionCreateHandler(discordClient);
  registerChannelDeleteHandler(discordClient);
  registerGuildBanAddHandler(discordClient);

  await discordClient.login(token);
}

export function getDiscordStatus(): 'ready' | 'reconnecting' | 'disconnected' {
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
