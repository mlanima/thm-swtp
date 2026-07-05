import { redis } from '../config/redis.js';
import { logger } from '../config/logger.js';
import { PlatformEventType, } from '../types/events.js';
const INBOUND_STREAM = 'stream:platform:sync';
async function produce(type, payload) {
    try {
        await redis.xadd(INBOUND_STREAM, '*', 'type', type, 'payload', JSON.stringify(payload));
        logger.debug({ type }, 'produced platform event');
    }
    catch (err) {
        logger.error({ err, type }, 'failed to produce platform event');
    }
}
export const streamProducer = {
    discordMessageAssigned(payload) {
        return produce(PlatformEventType.DiscordMessageAssigned, payload);
    },
    discordMessageCreated(payload) {
        return produce(PlatformEventType.DiscordMessageCreated, payload);
    },
    discordMessageUpdated(payload) {
        return produce(PlatformEventType.DiscordMessageUpdated, payload);
    },
    discordMessageDeleted(payload) {
        return produce(PlatformEventType.DiscordMessageDeleted, payload);
    },
    inviteResponse(payload) {
        return produce(PlatformEventType.InviteResponse, payload);
    },
    channelDisconnected(payload) {
        return produce(PlatformEventType.ChannelDisconnected, payload);
    },
};
//# sourceMappingURL=producer.js.map