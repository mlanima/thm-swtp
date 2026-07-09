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

const INBOUND_STREAM = 'stream:platform:sync';

async function produce(type: PlatformEventType, payload: Record<string, unknown>): Promise<void> {
  try {
    await redis.xadd(INBOUND_STREAM, '*', 'type', type, 'payload', JSON.stringify(payload));
    logger.debug({ type }, 'produced platform event');
  } catch (err) {
    logger.error({ err, type }, 'failed to produce platform event');
  }
}

export const streamProducer = {
  discordMessageAssigned(payload: DiscordMessageAssignedPayload) {
    return produce(PlatformEventType.DiscordMessageAssigned, payload as unknown as Record<string, unknown>);
  },

  discordMessageCreated(payload: DiscordMessageCreatedPayload) {
    return produce(PlatformEventType.DiscordMessageCreated, payload as unknown as Record<string, unknown>);
  },

  discordMessageUpdated(payload: DiscordMessageUpdatedPayload) {
    return produce(PlatformEventType.DiscordMessageUpdated, payload as unknown as Record<string, unknown>);
  },

  discordMessageDeleted(payload: DiscordMessageDeletedPayload) {
    return produce(PlatformEventType.DiscordMessageDeleted, payload as unknown as Record<string, unknown>);
  },

  inviteResponse(payload: InviteResponsePayload) {
    return produce(PlatformEventType.InviteResponse, payload as unknown as Record<string, unknown>);
  },

  channelDisconnected(payload: ChannelDisconnectedPayload) {
    return produce(PlatformEventType.ChannelDisconnected, payload as unknown as Record<string, unknown>);
  },
};
