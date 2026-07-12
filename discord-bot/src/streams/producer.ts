import { redis } from '../config/redis.js';
import { logger } from '../config/logger.js';
import {
  PlatformEventType,
  type DiscordMessageAssignedPayload,
  type DiscordMessageCreatedPayload,
  type DiscordMessageUpdatedPayload,
  type DiscordMessageDeletedPayload,
  type InviteResponsePayload,
  type ChannelDisconnectedPayload,
} from '../types/events.js';

/** Redis stream key where the bot writes events back to the platform. */
const INBOUND_STREAM = 'stream:platform:sync';

type PlatformPayload =
  | DiscordMessageAssignedPayload
  | DiscordMessageCreatedPayload
  | DiscordMessageUpdatedPayload
  | DiscordMessageDeletedPayload
  | InviteResponsePayload
  | ChannelDisconnectedPayload;

/**
 * Converts a typed payload into a plain object for Redis.
 * Avoids `as unknown as Record<string, unknown>`.
 */
function serializePayload(payload: PlatformPayload): Record<string, unknown> {
  const result: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(payload)) {
    result[key] = value instanceof Array ? value : value;
  }
  return result;
}

/** Writes a (type, payload) entry to the platform stream via XADD. */
async function produce(type: PlatformEventType, payload: PlatformPayload): Promise<void> {
  try {
    await redis.xadd(INBOUND_STREAM, '*', 'type', type, 'payload', JSON.stringify(serializePayload(payload)));
    logger.debug({ type }, 'produced platform event');
  } catch (err) {
    logger.error({ err, type }, 'failed to produce platform event');
  }
}

/** Convenience object with named methods for each platform event type. */
export const streamProducer = {
  discordMessageAssigned(payload: DiscordMessageAssignedPayload) {
    return produce(PlatformEventType.DiscordMessageAssigned, payload);
  },

  discordMessageCreated(payload: DiscordMessageCreatedPayload) {
    return produce(PlatformEventType.DiscordMessageCreated, payload);
  },

  discordMessageUpdated(payload: DiscordMessageUpdatedPayload) {
    return produce(PlatformEventType.DiscordMessageUpdated, payload);
  },

  discordMessageDeleted(payload: DiscordMessageDeletedPayload) {
    return produce(PlatformEventType.DiscordMessageDeleted, payload);
  },

  inviteResponse(payload: InviteResponsePayload) {
    return produce(PlatformEventType.InviteResponse, payload);
  },

  channelDisconnected(payload: ChannelDisconnectedPayload) {
    return produce(PlatformEventType.ChannelDisconnected, payload);
  },
};
