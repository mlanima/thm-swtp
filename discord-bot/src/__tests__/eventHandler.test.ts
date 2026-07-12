import { describe, it, expect, vi, beforeEach } from 'vitest';
import { createEventHandlers } from '../streams/eventHandler.js';
import type { Client, TextChannel, Message } from 'discord.js';
import type { Logger } from 'pino';
import type { Counter } from 'prom-client';

vi.mock('../bot/services/channelService.js', () => ({
  resolveSendableChannel: vi.fn(),
}));

import { resolveSendableChannel } from '../bot/services/channelService.js';

function createChannelStub(overrides?: Partial<TextChannel>): TextChannel {
  return {
    id: 'ch-1',
    guildId: 'guild-1',
    isDMBased: () => false,
    isTextBased: () => true,
    send: vi.fn().mockResolvedValue({ id: 'discord-msg-1', embeds: [] }),
    messages: {
      fetch: vi.fn().mockResolvedValue({
        id: 'discord-msg-1',
        embeds: [{
          data: { title: 'Old', description: 'Old content', color: 0x99AAB5 },
        }],
        edit: vi.fn().mockResolvedValue(undefined),
        delete: vi.fn().mockResolvedValue(undefined),
      }),
    },
    ...overrides,
  } as unknown as TextChannel;
}

function createMocks() {
  const client = {
    channels: {
      resolve: vi.fn(),
    },
    users: {
      fetch: vi.fn(),
    },
  } as unknown as Client;

  const streamProducer = {
    discordMessageAssigned: vi.fn().mockResolvedValue(undefined),
  };

  const logger = {
    info: vi.fn(),
    warn: vi.fn(),
    error: vi.fn(),
    debug: vi.fn(),
    fatal: vi.fn(),
  } as unknown as Logger;

  const messagesSent = {
    inc: vi.fn(),
  } as unknown as Counter<string>;

  const handlers = createEventHandlers({ discordClient: client, streamProducer, logger, messagesSent });

  return { client, streamProducer, logger, messagesSent, handlers };
}

describe('handleSendPost', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('sends an embed to the resolved channel', async () => {
    const { streamProducer, handlers } = createMocks();
    const channel = createChannelStub();
    vi.mocked(resolveSendableChannel).mockReturnValue(channel as any);

    const job = {
      data: {
        postId: 'post-1',
        channelId: 'ch-1',
        content: 'Hello',
        title: 'Post Title',
        authorName: 'Alice',
        authorAvatar: 'https://cdn.example.com/avatar.png',
        platformUrl: 'https://example.com/p/1',
      },
    } as any;

    await handlers.handleSendPost(job);

    expect(channel.send).toHaveBeenCalledOnce();
    expect(streamProducer.discordMessageAssigned).toHaveBeenCalledWith({
      postId: 'post-1',
      discordMsgId: 'discord-msg-1',
      channelId: 'ch-1',
      guildId: 'guild-1',
    });
  });

  it('throws when channel is not found', async () => {
    const { handlers } = createMocks();
    vi.mocked(resolveSendableChannel).mockReturnValue(null);

    const job = { data: { channelId: 'unknown', postId: '', content: '', title: '', authorName: '', platformUrl: '' } } as any;

    await expect(handlers.handleSendPost(job)).rejects.toThrow('Channel unknown not found');
  });
});

describe('handleEditPost', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('edits an existing message embed', async () => {
    const { handlers } = createMocks();
    const channel = createChannelStub();
    vi.mocked(resolveSendableChannel).mockReturnValue(channel as any);

    const job = { data: { discordMsgId: 'msg-1', channelId: 'ch-1', content: 'Updated' } } as any;

    await handlers.handleEditPost(job);

    const msg = await channel.messages.fetch('msg-1');
    expect(msg.edit).toHaveBeenCalledOnce();
  });

  it('throws when channel is not found', async () => {
    const { handlers } = createMocks();
    vi.mocked(resolveSendableChannel).mockReturnValue(null);

    const job = { data: { discordMsgId: 'msg-1', channelId: 'unknown', content: '' } } as any;

    await expect(handlers.handleEditPost(job)).rejects.toThrow('Channel unknown not found');
  });
});

describe('handleDeletePost', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('deletes the message', async () => {
    const { handlers } = createMocks();
    const channel = createChannelStub();
    vi.mocked(resolveSendableChannel).mockReturnValue(channel as any);

    const job = { data: { discordMsgId: 'msg-1', channelId: 'ch-1', postId: 'post-1' } } as any;

    await handlers.handleDeletePost(job);

    const msg = await channel.messages.fetch('msg-1');
    expect(msg.delete).toHaveBeenCalledOnce();
  });

  it('does nothing when channel not found', async () => {
    const { handlers } = createMocks();
    vi.mocked(resolveSendableChannel).mockReturnValue(null);

    const job = { data: { discordMsgId: 'msg-1', channelId: 'unknown', postId: 'post-1' } } as any;

    await expect(handlers.handleDeletePost(job)).resolves.toBeUndefined();
  });
});

describe('handleSendInvite', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('sends a DM with invite buttons', async () => {
    const { client, handlers } = createMocks();
    const user = { send: vi.fn().mockResolvedValue(undefined) };
    vi.mocked(client.users.fetch).mockResolvedValue(user as any);

    const job = {
      data: {
        inviteId: 'inv-1',
        targetDiscordId: 'user-1',
        projectName: 'Test',
        inviterName: 'Alice',
      },
    } as any;

    await handlers.handleSendInvite(job);

    expect(user.send).toHaveBeenCalledOnce();
  });

  it('skips when user cannot be fetched', async () => {
    const { client, logger, handlers } = createMocks();
    vi.mocked(client.users.fetch).mockRejectedValue(new Error('not found'));

    const job = {
      data: { inviteId: 'inv-1', targetDiscordId: 'unknown', projectName: 'Test', inviterName: 'Alice' },
    } as any;

    await handlers.handleSendInvite(job);

    expect(logger.warn).toHaveBeenCalled();
  });
});

describe('handleSendEvent', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('sends an event notification to the channel', async () => {
    const { handlers } = createMocks();
    const channel = createChannelStub();
    vi.mocked(resolveSendableChannel).mockReturnValue(channel as any);

    const job = {
      data: {
        channelId: 'ch-1',
        projectName: 'Test',
        message: 'New member!',
        eventType: 'MEMBER_JOIN',
      },
    } as any;

    await handlers.handleSendEvent(job);

    expect(channel.send).toHaveBeenCalledOnce();
  });

  it('throws when channel not found', async () => {
    const { handlers } = createMocks();
    vi.mocked(resolveSendableChannel).mockReturnValue(null);

    const job = {
      data: { channelId: 'unknown', projectName: 'Test', message: 'Hi', eventType: 'MEMBER_JOIN' },
    } as any;

    await expect(handlers.handleSendEvent(job)).rejects.toThrow('Channel unknown not found');
  });
});
