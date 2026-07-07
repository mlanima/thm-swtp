import { describe, it, expect } from 'vitest';
import { validateStreamMessage } from '../streams/validator.js';

function validPostCreated() {
  return {
    type: 'POST_CREATED' as const,
    payload: {
      postId: '550e8400-e29b-41d4-a716-446655440000',
      projectId: '660e8400-e29b-41d4-a716-446655440001',
      channelId: '123456789012345678',
      content: 'Hello from the platform!',
      authorName: 'testuser',
      platformUrl: 'https://swtp-ss26.de/project/test-project',
    },
  };
}

function validPostUpdated() {
  return {
    type: 'POST_UPDATED' as const,
    payload: {
      postId: '550e8400-e29b-41d4-a716-446655440000',
      discordMsgId: '987654321098765432',
      channelId: '123456789012345678',
      content: 'Updated content',
    },
  };
}

function validPostDeleted() {
  return {
    type: 'POST_DELETED' as const,
    payload: {
      postId: '550e8400-e29b-41d4-a716-446655440000',
      discordMsgId: '987654321098765432',
      channelId: '123456789012345678',
    },
  };
}

function validProjectInvite() {
  return {
    type: 'PROJECT_INVITE' as const,
    payload: {
      inviteId: '550e8400-e29b-41d4-a716-446655440000',
      targetDiscordId: '123456789012345678',
      projectName: 'My Test Project',
      inviterName: 'alice',
    },
  };
}

function validProjectEvent() {
  return {
    type: 'PROJECT_EVENT' as const,
    payload: {
      eventType: 'MEMBER_JOIN',
      projectId: '550e8400-e29b-41d4-a716-446655440000',
      projectName: 'My Test Project',
      channelId: '123456789012345678',
      message: 'A new member joined!',
    },
  };
}

function extractPayload<T extends { payload: unknown }>(msg: T): T['payload'] {
  return msg.payload;
}

describe('validateStreamMessage', () => {
  describe('POST_CREATED', () => {
    it('accepts a valid payload', () => {
      const result = validateStreamMessage(validPostCreated());
      expect(result.type).toBe('POST_CREATED');
      if (result.type === 'POST_CREATED') {
        expect(result.payload.platformUrl).toBe('https://swtp-ss26.de/project/test-project');
      }
    });

    it('accepts optional authorAvatar', () => {
      const input = {
        ...validPostCreated(),
        payload: { ...validPostCreated().payload, authorAvatar: 'https://cdn.discord.com/avatars/123/abc.png' },
      };
      const result = validateStreamMessage(input);
      if (result.type === 'POST_CREATED') {
        expect(result.payload.authorAvatar).toBe('https://cdn.discord.com/avatars/123/abc.png');
      }
    });

    it('rejects missing postId', () => {
      const input = { ...validPostCreated(), payload: extractPayload(validPostCreated()) };
      const { postId: _, ...payload } = input.payload;
      expect(() => validateStreamMessage({ type: 'POST_CREATED', payload })).toThrow();
    });

    it('rejects invalid UUID in postId', () => {
      expect(() => validateStreamMessage({
        ...validPostCreated(),
        payload: { ...validPostCreated().payload, postId: 'not-a-uuid' },
      })).toThrow();
    });

    it('rejects non-url platformUrl', () => {
      expect(() => validateStreamMessage({
        ...validPostCreated(),
        payload: { ...validPostCreated().payload, platformUrl: 'not-a-url' },
      })).toThrow();
    });

    it('rejects authorName exceeding 100 chars', () => {
      expect(() => validateStreamMessage({
        ...validPostCreated(),
        payload: { ...validPostCreated().payload, authorName: 'a'.repeat(101) },
      })).toThrow();
    });

    it('rejects content exceeding 3900 chars', () => {
      expect(() => validateStreamMessage({
        ...validPostCreated(),
        payload: { ...validPostCreated().payload, content: 'a'.repeat(3901) },
      })).toThrow();
    });
  });

  describe('POST_UPDATED', () => {
    it('accepts a valid payload', () => {
      const result = validateStreamMessage(validPostUpdated());
      expect(result.type).toBe('POST_UPDATED');
    });

    it('rejects missing content', () => {
      const { content: _, ...payload } = validPostUpdated().payload;
      expect(() => validateStreamMessage({ type: 'POST_UPDATED', payload })).toThrow();
    });
  });

  describe('POST_DELETED', () => {
    it('accepts a valid payload', () => {
      const result = validateStreamMessage(validPostDeleted());
      expect(result.type).toBe('POST_DELETED');
    });

    it('rejects missing discordMsgId', () => {
      const { discordMsgId: _, ...payload } = validPostDeleted().payload;
      expect(() => validateStreamMessage({ type: 'POST_DELETED', payload })).toThrow();
    });
  });

  describe('PROJECT_INVITE', () => {
    it('accepts a valid payload', () => {
      const result = validateStreamMessage(validProjectInvite());
      expect(result.type).toBe('PROJECT_INVITE');
    });

    it('rejects projectName exceeding 200 chars', () => {
      expect(() => validateStreamMessage({
        ...validProjectInvite(),
        payload: { ...validProjectInvite().payload, projectName: 'a'.repeat(201) },
      })).toThrow();
    });
  });

  describe('PROJECT_EVENT', () => {
    it('accepts a valid payload', () => {
      const result = validateStreamMessage(validProjectEvent());
      expect(result.type).toBe('PROJECT_EVENT');
    });

    it('rejects missing eventType', () => {
      const { eventType: _, ...payload } = validProjectEvent().payload;
      expect(() => validateStreamMessage({ type: 'PROJECT_EVENT', payload })).toThrow();
    });
  });

  describe('discriminated union', () => {
    it('rejects unknown event type', () => {
      expect(() => validateStreamMessage({ type: 'POST_SOMETHING', payload: {} })).toThrow();
    });

    it('rejects completely empty input', () => {
      expect(() => validateStreamMessage({})).toThrow();
    });

    it('rejects null', () => {
      expect(() => validateStreamMessage(null)).toThrow();
    });
  });
});
