export declare enum StreamEventType {
    PostCreated = "POST_CREATED",
    PostUpdated = "POST_UPDATED",
    PostDeleted = "POST_DELETED",
    ProjectInvite = "PROJECT_INVITE",
    ProjectEvent = "PROJECT_EVENT"
}
export interface PostCreatedPayload {
    postId: string;
    projectId: string;
    channelId: string;
    content: string;
    authorName: string;
    authorAvatar?: string;
    platformUrl: string;
}
export interface PostUpdatedPayload {
    postId: string;
    discordMsgId: string;
    channelId: string;
    content: string;
}
export interface PostDeletedPayload {
    postId: string;
    discordMsgId: string;
    channelId: string;
}
export interface ProjectInvitePayload {
    inviteId: string;
    targetDiscordId: string;
    projectName: string;
    inviterName: string;
}
export interface ProjectEventPayload {
    eventType: string;
    projectId: string;
    projectName: string;
    channelId: string;
    message: string;
}
export type StreamPayload = PostCreatedPayload | PostUpdatedPayload | PostDeletedPayload | ProjectInvitePayload | ProjectEventPayload;
export interface StreamMessage {
    type: StreamEventType;
    payload: StreamPayload;
}
export declare enum PlatformEventType {
    DiscordMessageAssigned = "DISCORD_MESSAGE_ASSIGNED",
    DiscordMessageCreated = "DISCORD_MESSAGE_CREATED",
    DiscordMessageUpdated = "DISCORD_MESSAGE_UPDATED",
    DiscordMessageDeleted = "DISCORD_MESSAGE_DELETED",
    InviteResponse = "INVITE_RESPONSE",
    ChannelDisconnected = "CHANNEL_DISCONNECTED"
}
export interface DiscordMessageAssignedPayload {
    postId: string;
    discordMsgId: string;
    channelId: string;
}
export interface DiscordMessageCreatedPayload {
    discordMsgId: string;
    channelId: string;
    content: string;
    discordUserId: string;
    discordUsername: string;
    attachmentUrls?: string[];
}
export interface DiscordMessageUpdatedPayload {
    discordMsgId: string;
    content: string;
}
export interface DiscordMessageDeletedPayload {
    discordMsgId: string;
}
export interface InviteResponsePayload {
    inviteId: string;
    response: 'ACCEPTED' | 'DECLINED';
}
export interface ChannelDisconnectedPayload {
    channelId: string;
    reason: string;
}
export type PlatformPayload = DiscordMessageAssignedPayload | DiscordMessageCreatedPayload | DiscordMessageUpdatedPayload | DiscordMessageDeletedPayload | InviteResponsePayload | ChannelDisconnectedPayload;
export interface PlatformMessage {
    type: PlatformEventType;
    payload: PlatformPayload;
}
//# sourceMappingURL=events.d.ts.map