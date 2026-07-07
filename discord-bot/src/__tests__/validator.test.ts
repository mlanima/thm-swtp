import { describe, it, expect } from 'vitest';
import { validateStreamMessage } from '../streams/validator.js';

function validPostCreated() {
  return {
    type: 'POST_CREATED',
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
    type: 'POST_UPDATED',
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
    type: 'POST_DELETED',
    payload: {
      postId: '550e8400-e29b-41d4-a716-446655440000',
      discordMsgId: '987654321098765432',
      channelId: '123456789012345678',
    },
  };
}

function validProjectInvite() {
  return {
    type: 'PROJECT_INVITE',
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
    type: 'PROJECT_EVENT',
    payload: {
      eventType: 'MEMBER_JOIN',
      projectId: '550e8400-e29b-41d4-a716-446655440000',
      projectName: 'My Test Project',
      channelId: '123456789012345678',
      message: 'A new member joined!',
    },
  };
}

describe('validateStreamMessage', () => {
  describe('POST_CREATED', () => {
    it('accepts a valid payload', () => {
      const result = validateStreamMessage(validPostCreated());
      expect(result.type).toBe('POST_CREATED');
      expect(result.payload.platformUrl).toBe('https://swtp-ss26.de/project/test-project');
    });

    it('accepts optional authorAvatar', () => {
      const msg = validPostCreated();
      msg.payload.authorAvatar = 'https://cdn.discord.com/avatars/123/abc.png';
      const result = validateStreamMessage(msg);
      expect(result.payload.authorAvatar).toBe('https://cdn.discord.com/avatars/123/abc.png');
    });

    it('rejects missing postId', () => {
      const msg = validPostCreated();
      delete (msg.payload as any).postId;
      expect(() => validateStreamMessage(msg)).toThrow();
    });

    it('rejects invalid UUID in postId', () => {
      const msg = validPostCreated();
      msg.payload.postId = 'not-a-uuid';
      expect(() => validateStreamMessage(msg)).toThrow();
    });

    it('rejects non-url platformUrl', () => {
      const msg = validPostCreated();
      msg.payload.platformUrl = 'not-a-url';
      expect(() => validateStreamMessage(msg)).toThrow();
    });

    it('rejects authorName exceeding 100 chars', () => {
      const msg = validPostCreated();
      msg.payload.authorName = 'a'.repeat(101);
      expect(() => validateStreamMessage(msg)).toThrow();
    });

    it('rejects content exceeding 3900 chars', () => {
      const msg = validPostCreated();
      msg.payload.content = 'a'.repeat(3901);
      expect(() => validateStreamMessage(msg)).toThrow();
    });
  });

  describe('POST_UPDATED', () => {
    it('accepts a valid payload', () => {
      const result = validateStreamMessage(validPostUpdated());
      expect(result.type).toBe('POST_UPDATED');
    });

    it('rejects missing content', () => {
      const msg = validPostUpdated();
      delete (msg.payload as any).content;
      expect(() => validateStreamMessage(msg)).toThrow();
    });
  });

  describe('POST_DELETED', () => {
    it('accepts a valid payload', () => {
      const result = validateStreamMessage(validPostDeleted());
      expect(result.type).toBe('POST_DELETED');
    });

    it('rejects missing discordMsgId', () => {
      const msg = validPostDeleted();
      delete (msg.payload as any).discordMsgId;
      expect(() => validateStreamMessage(msg)).toThrow();
    });
  });

  describe('PROJECT_INVITE', () => {
    it('accepts a valid payload', () => {
      const result = validateStreamMessage(validProjectInvite());
      expect(result.type).toBe('PROJECT_INVITE');
    });

    it('rejects projectName exceeding 200 chars', () => {
      const msg = validProjectInvite();
      msg.payload.projectName = 'a'.repeat(201);
      expect(() => validateStreamMessage(msg)).toThrow();
    });
  });

  describe('PROJECT_EVENT', () => {
    it('accepts a valid payload', () => {
      const result = validateStreamMessage(validProjectEvent());
      expect(result.type).toBe('PROJECT_EVENT');
    });

    it('rejects missing eventType', () => {
      const msg = validProjectEvent();
      delete (msg.payload as any).eventType;
      expect(() => validateStreamMessage(msg)).toThrow();
    });
  });

  describe('discriminated union', () => {
    it('rejects unknown event type', () => {
      const msg = { type: 'POST_SOMETHING', payload: {} };
      expect(() => validateStreamMessage(msg)).toThrow();
    });

    it('rejects completely empty input', () => {
      expect(() => validateStreamMessage({})).toThrow();
    });

    it('rejects null', () => {
      expect(() => validateStreamMessage(null)).toThrow();
    });
  });
});
