import { type DiscordMessageAssignedPayload, type DiscordMessageCreatedPayload, type DiscordMessageUpdatedPayload, type DiscordMessageDeletedPayload, type InviteResponsePayload, type ChannelDisconnectedPayload } from '../types/events.js';
export declare const streamProducer: {
    discordMessageAssigned(payload: DiscordMessageAssignedPayload): Promise<void>;
    discordMessageCreated(payload: DiscordMessageCreatedPayload): Promise<void>;
    discordMessageUpdated(payload: DiscordMessageUpdatedPayload): Promise<void>;
    discordMessageDeleted(payload: DiscordMessageDeletedPayload): Promise<void>;
    inviteResponse(payload: InviteResponsePayload): Promise<void>;
    channelDisconnected(payload: ChannelDisconnectedPayload): Promise<void>;
};
//# sourceMappingURL=producer.d.ts.map