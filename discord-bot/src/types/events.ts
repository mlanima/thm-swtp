// ── Stream → Bot (platform sends events to the bot) ──

/** Event types the platform can send to the bot via the Redis stream. */
export enum StreamEventType {
  PostCreated = 'POST_CREATED',
  PostUpdated = 'POST_UPDATED',
  PostDeleted = 'POST_DELETED',
  ProjectInvite = 'PROJECT_INVITE',
  ProjectEvent = 'PROJECT_EVENT',
}

/** Payload for POST_CREATED — a new forum post was created on the platform. */
export interface PostCreatedPayload {
  postId: string;
  projectId: string;
  channelId: string;
  content: string;
  title: string;
  authorName: string;
  authorAvatar?: string;
  platformUrl: string;
}

/** Payload for POST_UPDATED — a post's content changed. */
export interface PostUpdatedPayload {
  postId: string;
  discordMsgId: string;
  channelId: string;
  content: string;
}

/** Payload for POST_DELETED — a post was removed. */
export interface PostDeletedPayload {
  postId: string;
  discordMsgId: string;
  channelId: string;
}

/** Payload for PROJECT_INVITE — someone was invited to a project. */
export interface ProjectInvitePayload {
  inviteId: string;
  targetDiscordId: string;
  projectName: string;
  inviterName: string;
}

/** Payload for PROJECT_EVENT — a project-level notification (milestone, etc.). */
export interface ProjectEventPayload {
  eventType: string;
  projectId: string;
  projectName: string;
  channelId: string;
  message: string;
}

/** Union of all stream (platform→bot) payloads. */
export type StreamPayload =
  | PostCreatedPayload
  | PostUpdatedPayload
  | PostDeletedPayload
  | ProjectInvitePayload
  | ProjectEventPayload;

/** A full message on the platform-to-bot stream. */
export interface StreamMessage {
  type: StreamEventType;
  payload: StreamPayload;
}

// ── Bot → Platform (bot sends events back to the platform) ──

/** Event types the bot can send back to the platform. */
export enum PlatformEventType {
  DiscordMessageAssigned = 'DISCORD_MESSAGE_ASSIGNED',
  DiscordMessageCreated = 'DISCORD_MESSAGE_CREATED',
  DiscordMessageUpdated = 'DISCORD_MESSAGE_UPDATED',
  DiscordMessageDeleted = 'DISCORD_MESSAGE_DELETED',
  InviteResponse = 'INVITE_RESPONSE',
  ChannelDisconnected = 'CHANNEL_DISCONNECTED',
}

/** Payload for DISCORD_MESSAGE_ASSIGNED — the bot created a Discord message for a post. */
export interface DiscordMessageAssignedPayload {
  postId: string;
  discordMsgId: string;
  channelId: string;
  guildId?: string;
}

/** Payload for DISCORD_MESSAGE_CREATED — a user sent a message in a linked channel. */
export interface DiscordMessageCreatedPayload {
  discordMsgId: string;
  channelId: string;
  content: string;
  discordUserId: string;
  discordUsername: string;
  attachmentUrls?: string[];
}

/** Payload for DISCORD_MESSAGE_UPDATED — a user edited their message. */
export interface DiscordMessageUpdatedPayload {
  discordMsgId: string;
  content: string;
}

/** Payload for DISCORD_MESSAGE_DELETED — a user deleted their message. */
export interface DiscordMessageDeletedPayload {
  discordMsgId: string;
}

/** Payload for INVITE_RESPONSE — a user accepted or declined an invite via Discord buttons. */
export interface InviteResponsePayload {
  inviteId: string;
  response: 'ACCEPTED' | 'DECLINED';
}

/** Payload for CHANNEL_DISCONNECTED — the bot can no longer access a channel. */
export interface ChannelDisconnectedPayload {
  channelId: string;
  guildId?: string;
  reason: string;
}

/** Union of all platform (bot→platform) payloads. */
export type PlatformPayload =
  | DiscordMessageAssignedPayload
  | DiscordMessageCreatedPayload
  | DiscordMessageUpdatedPayload
  | DiscordMessageDeletedPayload
  | InviteResponsePayload
  | ChannelDisconnectedPayload;

/** A full message on the bot-to-platform stream. */
export interface PlatformMessage {
  type: PlatformEventType;
  payload: PlatformPayload;
}
