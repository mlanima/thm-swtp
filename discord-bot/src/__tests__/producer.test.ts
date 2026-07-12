import { describe, it, expect, vi, beforeEach } from 'vitest';
import { PlatformEventType } from '../types/events.js';

vi.mock('../config/redis.js', () => ({
  redis: {
    xadd: vi.fn().mockResolvedValue('mock-id'),
  },
}));

vi.mock('../config/logger.js', () => ({
  logger: {
    debug: vi.fn(),
    error: vi.fn(),
    info: vi.fn(),
    warn: vi.fn(),
  },
}));

const { streamProducer } = await import('../streams/producer.js');
const { redis } = await import('../config/redis.js');

describe('streamProducer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('discordMessageAssigned produces correct event', async () => {
    await streamProducer.discordMessageAssigned({
      postId: 'post-1',
      discordMsgId: 'msg-1',
      channelId: 'ch-1',
      guildId: 'guild-1',
    });

    expect(redis.xadd).toHaveBeenCalledWith(
      'stream:platform:sync', '*',
      'type', PlatformEventType.DiscordMessageAssigned,
      'payload', expect.any(String),
    );
  });

  it('discordMessageCreated produces correct event', async () => {
    await streamProducer.discordMessageCreated({
      discordMsgId: 'msg-1',
      channelId: 'ch-1',
      content: 'Hello',
      discordUserId: 'user-1',
      discordUsername: 'Alice',
      attachmentUrls: ['https://cdn.example.com/img.png'],
    });

    expect(redis.xadd).toHaveBeenCalledWith(
      'stream:platform:sync', '*',
      'type', PlatformEventType.DiscordMessageCreated,
      'payload', expect.any(String),
    );
  });

  it('discordMessageUpdated produces correct event', async () => {
    await streamProducer.discordMessageUpdated({
      discordMsgId: 'msg-1',
      content: 'Updated',
    });

    expect(redis.xadd).toHaveBeenCalledWith(
      'stream:platform:sync', '*',
      'type', PlatformEventType.DiscordMessageUpdated,
      'payload', expect.any(String),
    );
  });

  it('discordMessageDeleted produces correct event', async () => {
    await streamProducer.discordMessageDeleted({ discordMsgId: 'msg-1' });

    expect(redis.xadd).toHaveBeenCalledWith(
      'stream:platform:sync', '*',
      'type', PlatformEventType.DiscordMessageDeleted,
      'payload', expect.any(String),
    );
  });

  it('inviteResponse produces correct event', async () => {
    await streamProducer.inviteResponse({ inviteId: 'inv-1', response: 'ACCEPTED' });

    expect(redis.xadd).toHaveBeenCalledWith(
      'stream:platform:sync', '*',
      'type', PlatformEventType.InviteResponse,
      'payload', expect.any(String),
    );
  });

  it('channelDisconnected produces correct event', async () => {
    await streamProducer.channelDisconnected({ channelId: 'ch-1', reason: 'BOT_BANNED' });

    expect(redis.xadd).toHaveBeenCalledWith(
      'stream:platform:sync', '*',
      'type', PlatformEventType.ChannelDisconnected,
      'payload', expect.any(String),
    );
  });

  it('serializes payload as valid JSON', async () => {
    await streamProducer.discordMessageCreated({
      discordMsgId: 'msg-1',
      channelId: 'ch-1',
      content: 'Hello',
      discordUserId: 'user-1',
      discordUsername: 'Alice',
    });

    const callArgs = vi.mocked(redis.xadd).mock.calls[0];
    const payloadArg = callArgs[5] as string;
    const parsed = JSON.parse(payloadArg);
    expect(parsed.discordMsgId).toBe('msg-1');
    expect(parsed.content).toBe('Hello');
  });
});
