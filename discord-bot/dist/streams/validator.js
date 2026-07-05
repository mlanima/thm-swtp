import { z } from 'zod';
export const PostCreatedSchema = z.object({
    postId: z.string().uuid(),
    projectId: z.string().uuid(),
    channelId: z.string(),
    content: z.string().max(3900),
    authorName: z.string().max(100),
    authorAvatar: z.string().url().optional(),
    platformUrl: z.string().url(),
});
export const PostUpdatedSchema = z.object({
    postId: z.string().uuid(),
    discordMsgId: z.string(),
    channelId: z.string(),
    content: z.string().max(3900),
});
export const PostDeletedSchema = z.object({
    postId: z.string().uuid(),
    discordMsgId: z.string(),
    channelId: z.string(),
});
export const ProjectInviteSchema = z.object({
    inviteId: z.string().uuid(),
    targetDiscordId: z.string(),
    projectName: z.string().max(200),
    inviterName: z.string().max(100),
});
export const ProjectEventSchema = z.object({
    eventType: z.string().max(50),
    projectId: z.string().uuid(),
    projectName: z.string().max(200),
    channelId: z.string(),
    message: z.string().max(3900),
});
export const StreamPayloadSchema = z.discriminatedUnion('type', [
    z.object({ type: z.literal('POST_CREATED'), payload: PostCreatedSchema }),
    z.object({ type: z.literal('POST_UPDATED'), payload: PostUpdatedSchema }),
    z.object({ type: z.literal('POST_DELETED'), payload: PostDeletedSchema }),
    z.object({ type: z.literal('PROJECT_INVITE'), payload: ProjectInviteSchema }),
    z.object({ type: z.literal('PROJECT_EVENT'), payload: ProjectEventSchema }),
]);
export function validateStreamMessage(raw) {
    return StreamPayloadSchema.parse(raw);
}
//# sourceMappingURL=validator.js.map