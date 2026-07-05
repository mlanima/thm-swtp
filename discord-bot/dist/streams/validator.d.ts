import { z } from 'zod';
export declare const PostCreatedSchema: z.ZodObject<{
    postId: z.ZodString;
    projectId: z.ZodString;
    channelId: z.ZodString;
    content: z.ZodString;
    authorName: z.ZodString;
    authorAvatar: z.ZodOptional<z.ZodString>;
    platformUrl: z.ZodString;
}, "strip", z.ZodTypeAny, {
    content: string;
    channelId: string;
    postId: string;
    projectId: string;
    authorName: string;
    platformUrl: string;
    authorAvatar?: string | undefined;
}, {
    content: string;
    channelId: string;
    postId: string;
    projectId: string;
    authorName: string;
    platformUrl: string;
    authorAvatar?: string | undefined;
}>;
export declare const PostUpdatedSchema: z.ZodObject<{
    postId: z.ZodString;
    discordMsgId: z.ZodString;
    channelId: z.ZodString;
    content: z.ZodString;
}, "strip", z.ZodTypeAny, {
    content: string;
    channelId: string;
    postId: string;
    discordMsgId: string;
}, {
    content: string;
    channelId: string;
    postId: string;
    discordMsgId: string;
}>;
export declare const PostDeletedSchema: z.ZodObject<{
    postId: z.ZodString;
    discordMsgId: z.ZodString;
    channelId: z.ZodString;
}, "strip", z.ZodTypeAny, {
    channelId: string;
    postId: string;
    discordMsgId: string;
}, {
    channelId: string;
    postId: string;
    discordMsgId: string;
}>;
export declare const ProjectInviteSchema: z.ZodObject<{
    inviteId: z.ZodString;
    targetDiscordId: z.ZodString;
    projectName: z.ZodString;
    inviterName: z.ZodString;
}, "strip", z.ZodTypeAny, {
    inviteId: string;
    targetDiscordId: string;
    projectName: string;
    inviterName: string;
}, {
    inviteId: string;
    targetDiscordId: string;
    projectName: string;
    inviterName: string;
}>;
export declare const ProjectEventSchema: z.ZodObject<{
    eventType: z.ZodString;
    projectId: z.ZodString;
    projectName: z.ZodString;
    channelId: z.ZodString;
    message: z.ZodString;
}, "strip", z.ZodTypeAny, {
    channelId: string;
    message: string;
    projectId: string;
    projectName: string;
    eventType: string;
}, {
    channelId: string;
    message: string;
    projectId: string;
    projectName: string;
    eventType: string;
}>;
export declare const StreamPayloadSchema: z.ZodDiscriminatedUnion<"type", [z.ZodObject<{
    type: z.ZodLiteral<"POST_CREATED">;
    payload: z.ZodObject<{
        postId: z.ZodString;
        projectId: z.ZodString;
        channelId: z.ZodString;
        content: z.ZodString;
        authorName: z.ZodString;
        authorAvatar: z.ZodOptional<z.ZodString>;
        platformUrl: z.ZodString;
    }, "strip", z.ZodTypeAny, {
        content: string;
        channelId: string;
        postId: string;
        projectId: string;
        authorName: string;
        platformUrl: string;
        authorAvatar?: string | undefined;
    }, {
        content: string;
        channelId: string;
        postId: string;
        projectId: string;
        authorName: string;
        platformUrl: string;
        authorAvatar?: string | undefined;
    }>;
}, "strip", z.ZodTypeAny, {
    type: "POST_CREATED";
    payload: {
        content: string;
        channelId: string;
        postId: string;
        projectId: string;
        authorName: string;
        platformUrl: string;
        authorAvatar?: string | undefined;
    };
}, {
    type: "POST_CREATED";
    payload: {
        content: string;
        channelId: string;
        postId: string;
        projectId: string;
        authorName: string;
        platformUrl: string;
        authorAvatar?: string | undefined;
    };
}>, z.ZodObject<{
    type: z.ZodLiteral<"POST_UPDATED">;
    payload: z.ZodObject<{
        postId: z.ZodString;
        discordMsgId: z.ZodString;
        channelId: z.ZodString;
        content: z.ZodString;
    }, "strip", z.ZodTypeAny, {
        content: string;
        channelId: string;
        postId: string;
        discordMsgId: string;
    }, {
        content: string;
        channelId: string;
        postId: string;
        discordMsgId: string;
    }>;
}, "strip", z.ZodTypeAny, {
    type: "POST_UPDATED";
    payload: {
        content: string;
        channelId: string;
        postId: string;
        discordMsgId: string;
    };
}, {
    type: "POST_UPDATED";
    payload: {
        content: string;
        channelId: string;
        postId: string;
        discordMsgId: string;
    };
}>, z.ZodObject<{
    type: z.ZodLiteral<"POST_DELETED">;
    payload: z.ZodObject<{
        postId: z.ZodString;
        discordMsgId: z.ZodString;
        channelId: z.ZodString;
    }, "strip", z.ZodTypeAny, {
        channelId: string;
        postId: string;
        discordMsgId: string;
    }, {
        channelId: string;
        postId: string;
        discordMsgId: string;
    }>;
}, "strip", z.ZodTypeAny, {
    type: "POST_DELETED";
    payload: {
        channelId: string;
        postId: string;
        discordMsgId: string;
    };
}, {
    type: "POST_DELETED";
    payload: {
        channelId: string;
        postId: string;
        discordMsgId: string;
    };
}>, z.ZodObject<{
    type: z.ZodLiteral<"PROJECT_INVITE">;
    payload: z.ZodObject<{
        inviteId: z.ZodString;
        targetDiscordId: z.ZodString;
        projectName: z.ZodString;
        inviterName: z.ZodString;
    }, "strip", z.ZodTypeAny, {
        inviteId: string;
        targetDiscordId: string;
        projectName: string;
        inviterName: string;
    }, {
        inviteId: string;
        targetDiscordId: string;
        projectName: string;
        inviterName: string;
    }>;
}, "strip", z.ZodTypeAny, {
    type: "PROJECT_INVITE";
    payload: {
        inviteId: string;
        targetDiscordId: string;
        projectName: string;
        inviterName: string;
    };
}, {
    type: "PROJECT_INVITE";
    payload: {
        inviteId: string;
        targetDiscordId: string;
        projectName: string;
        inviterName: string;
    };
}>, z.ZodObject<{
    type: z.ZodLiteral<"PROJECT_EVENT">;
    payload: z.ZodObject<{
        eventType: z.ZodString;
        projectId: z.ZodString;
        projectName: z.ZodString;
        channelId: z.ZodString;
        message: z.ZodString;
    }, "strip", z.ZodTypeAny, {
        channelId: string;
        message: string;
        projectId: string;
        projectName: string;
        eventType: string;
    }, {
        channelId: string;
        message: string;
        projectId: string;
        projectName: string;
        eventType: string;
    }>;
}, "strip", z.ZodTypeAny, {
    type: "PROJECT_EVENT";
    payload: {
        channelId: string;
        message: string;
        projectId: string;
        projectName: string;
        eventType: string;
    };
}, {
    type: "PROJECT_EVENT";
    payload: {
        channelId: string;
        message: string;
        projectId: string;
        projectName: string;
        eventType: string;
    };
}>]>;
export declare function validateStreamMessage(raw: unknown): {
    type: "POST_CREATED";
    payload: {
        content: string;
        channelId: string;
        postId: string;
        projectId: string;
        authorName: string;
        platformUrl: string;
        authorAvatar?: string | undefined;
    };
} | {
    type: "POST_UPDATED";
    payload: {
        content: string;
        channelId: string;
        postId: string;
        discordMsgId: string;
    };
} | {
    type: "POST_DELETED";
    payload: {
        channelId: string;
        postId: string;
        discordMsgId: string;
    };
} | {
    type: "PROJECT_INVITE";
    payload: {
        inviteId: string;
        targetDiscordId: string;
        projectName: string;
        inviterName: string;
    };
} | {
    type: "PROJECT_EVENT";
    payload: {
        channelId: string;
        message: string;
        projectId: string;
        projectName: string;
        eventType: string;
    };
};
//# sourceMappingURL=validator.d.ts.map