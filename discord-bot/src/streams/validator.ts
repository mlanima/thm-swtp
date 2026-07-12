import { z } from 'zod';

/** Validates that a post-creation payload has correct types and reasonable lengths. */
export const PostCreatedSchema = z.object({
  postId: z.string().uuid(),
  projectId: z.string().uuid(),
  channelId: z.string(),
  content: z.string().max(3900),
  title: z.string().max(200),
  authorName: z.string().max(100),
  authorAvatar: z.string().optional(),
  platformUrl: z.string().url(),
});

/** Validates a post-edit payload. */
export const PostUpdatedSchema = z.object({
  postId: z.string().uuid(),
  discordMsgId: z.string(),
  channelId: z.string(),
  content: z.string().max(3900),
});

/** Validates a post-deletion payload. */
export const PostDeletedSchema = z.object({
  postId: z.string().uuid(),
  discordMsgId: z.string(),
  channelId: z.string(),
});

/** Validates a project-invite payload. */
export const ProjectInviteSchema = z.object({
  inviteId: z.string().uuid(),
  targetDiscordId: z.string(),
  projectName: z.string().max(200),
  inviterName: z.string().max(100),
});

/** Validates a project-event payload (e.g. milestone reached). */
export const ProjectEventSchema = z.object({
  eventType: z.string().max(50),
  projectId: z.string().uuid(),
  projectName: z.string().max(200),
  channelId: z.string(),
  message: z.string().max(3900),
});

/** Discriminated union over all stream event types — each event has a `type` and `payload`. */
export const StreamPayloadSchema = z.discriminatedUnion('type', [
  z.object({ type: z.literal('POST_CREATED'), payload: PostCreatedSchema }),
  z.object({ type: z.literal('POST_UPDATED'), payload: PostUpdatedSchema }),
  z.object({ type: z.literal('POST_DELETED'), payload: PostDeletedSchema }),
  z.object({ type: z.literal('PROJECT_INVITE'), payload: ProjectInviteSchema }),
  z.object({ type: z.literal('PROJECT_EVENT'), payload: ProjectEventSchema }),
]);

/** Parses and validates a raw stream message. Throws on invalid data. */
export function validateStreamMessage(raw: unknown) {
  return StreamPayloadSchema.parse(raw);
}
