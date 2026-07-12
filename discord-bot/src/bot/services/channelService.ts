import { discordClient } from '../client.js';
import type { TextChannel, NewsChannel, ThreadChannel, GuildBasedChannel, NonThreadGuildBasedChannel } from 'discord.js';

/** Channels we can send messages to: text, news, or threads. */
export type SendableChannel = TextChannel | NewsChannel | ThreadChannel;

/** Resolves a channel ID to a SendableChannel, or returns null if it doesn't exist or can't be written to. */
export function resolveSendableChannel(channelId: string): SendableChannel | null {
  const channel = discordClient.channels.resolve(channelId);
  if (!channel) return null;
  // DMs are also SendableChannel under the hood (they have .send())
  if (channel.isDMBased()) return channel as unknown as SendableChannel;
  if (channel.isTextBased() && 'send' in channel) return channel as SendableChannel;
  return null;
}

/**
 * Fetches a guild text channel by ID.
 * Returns an error bundle instead of throwing if the channel doesn't exist or isn't a guild text channel.
 */
export async function resolveGuildChannel(channelId: string): Promise<{
  channel: GuildBasedChannel & NonThreadGuildBasedChannel;
  error: null;
} | {
  channel: null;
  error: { status: number; json: Record<string, unknown> };
}> {
  const channel = await discordClient.channels.fetch(channelId);
  if (!channel || !channel.isTextBased() || !('guild' in channel) || !channel.guild) {
    return {
      channel: null,
      error: { status: 200, json: { success: false, reason: 'channel not found or not a text channel in a guild' } },
    };
  }
  return { channel: channel as GuildBasedChannel & NonThreadGuildBasedChannel, error: null };
}
