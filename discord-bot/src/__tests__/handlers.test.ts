import { describe, it, expect, vi, beforeEach } from 'vitest';
import { Events } from 'discord.js';

vi.mock('../bot/wrapAsync.js', () => ({
  wrapAsync: (_fn: Function, _label?: string) => _fn,
}));

const mockRedisSet = vi.fn().mockResolvedValue('OK');
vi.mock('../config/redis.js', () => ({
  redis: {
    set: mockRedisSet,
  },
}));

vi.mock('../config/logger.js', () => ({
  logger: {
    info: vi.fn(),
    warn: vi.fn(),
    debug: vi.fn(),
    error: vi.fn(),
  },
}));

const streamProducerMock = {
  discordMessageCreated: vi.fn().mockResolvedValue(undefined),
  discordMessageUpdated: vi.fn().mockResolvedValue(undefined),
  discordMessageDeleted: vi.fn().mockResolvedValue(undefined),
  inviteResponse: vi.fn().mockResolvedValue(undefined),
  channelDisconnected: vi.fn().mockResolvedValue(undefined),
};

vi.mock('../streams/producer.js', () => ({
  streamProducer: streamProducerMock,
}));

const { registerMessageCreateHandler } = await import('../bot/handlers/messageCreate.js');
const { registerMessageUpdateHandler } = await import('../bot/handlers/messageUpdate.js');
const { registerMessageDeleteHandler } = await import('../bot/handlers/messageDelete.js');
const { registerInteractionCreateHandler } = await import('../bot/handlers/interactionCreate.js');
const { registerChannelDeleteHandler } = await import('../bot/handlers/channelDelete.js');
const { registerGuildBanAddHandler } = await import('../bot/handlers/guildBanAdd.js');

function createClientMock() {
  const handlers = new Map<string, Function>();
  return {
    on: vi.fn((event: string, handler: Function) => {
      handlers.set(event, handler);
    }),
    getHandler(event: string) {
      return handlers.get(event);
    },
    async emit(event: string, ...args: any[]) {
      const handler = handlers.get(event);
      if (handler) await handler(...args);
    },
    user: { id: 'bot-1' },
  };
}

describe('messageCreate handler', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('produces discordMessageCreated for non-bot messages', async () => {
    const client = createClientMock();
    registerMessageCreateHandler(client as any);

    const message = {
      author: { bot: false, id: 'user-1', username: 'Alice' },
      webhookId: null,
      guild: { id: 'guild-1' },
      id: 'msg-1',
      channelId: 'ch-1',
      content: 'Hello!',
      attachments: { map: vi.fn(() => []) },
    };

    await client.emit(Events.MessageCreate, message);

    expect(streamProducerMock.discordMessageCreated).toHaveBeenCalledWith({
      discordMsgId: 'msg-1',
      channelId: 'ch-1',
      content: 'Hello!',
      discordUserId: 'user-1',
      discordUsername: 'Alice',
      attachmentUrls: undefined,
    });
  });

  it('skips bot messages', async () => {
    const client = createClientMock();
    registerMessageCreateHandler(client as any);

    const message = {
      author: { bot: true, id: 'bot-2', username: 'OtherBot' },
      webhookId: null,
      guild: { id: 'guild-1' },
      id: 'msg-1',
      channelId: 'ch-1',
      content: 'Hello!',
      attachments: { map: vi.fn(() => []) },
    };

    await client.emit(Events.MessageCreate, message);

    expect(streamProducerMock.discordMessageCreated).not.toHaveBeenCalled();
  });

  it('skips messages without guild', async () => {
    const client = createClientMock();
    registerMessageCreateHandler(client as any);

    const message = {
      author: { bot: false, id: 'user-1', username: 'Alice' },
      webhookId: null,
      guild: null,
      id: 'msg-1',
      channelId: 'ch-1',
      content: 'Hello!',
      attachments: { map: vi.fn(() => []) },
    };

    await client.emit(Events.MessageCreate, message);

    expect(streamProducerMock.discordMessageCreated).not.toHaveBeenCalled();
  });

  it('includes attachment URLs when present', async () => {
    const client = createClientMock();
    registerMessageCreateHandler(client as any);

    const message = {
      author: { bot: false, id: 'user-1', username: 'Alice' },
      webhookId: null,
      guild: { id: 'guild-1' },
      id: 'msg-1',
      channelId: 'ch-1',
      content: 'With image',
      attachments: { map: vi.fn((fn: Function) => [fn({ url: 'https://cdn.example.com/img.png' })]) },
    };

    await client.emit(Events.MessageCreate, message);

    expect(streamProducerMock.discordMessageCreated).toHaveBeenCalledWith(
      expect.objectContaining({ attachmentUrls: ['https://cdn.example.com/img.png'] })
    );
  });
});

describe('messageUpdate handler', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('produces discordMessageUpdated for non-bot edits', async () => {
    const client = createClientMock();
    registerMessageUpdateHandler(client as any);

    const oldMessage = { id: 'msg-1', content: 'Old' };
    const newMessage = {
      author: { bot: false },
      webhookId: null,
      id: 'msg-1',
      partial: false,
      content: 'Updated',
    };

    await client.emit(Events.MessageUpdate, oldMessage, newMessage);

    expect(streamProducerMock.discordMessageUpdated).toHaveBeenCalledWith({
      discordMsgId: 'msg-1',
      content: 'Updated',
    });
  });

  it('fetches partial messages before processing', async () => {
    const client = createClientMock();
    registerMessageUpdateHandler(client as any);

    const oldMessage = { id: 'msg-1' };
    const newMessage = {
      author: { bot: false },
      webhookId: null,
      id: 'msg-1',
      partial: true,
      fetch: vi.fn().mockResolvedValue({
        id: 'msg-1',
        author: { bot: false },
        content: 'Fetched content',
      }),
    };

    await client.emit(Events.MessageUpdate, oldMessage, newMessage);

    expect(streamProducerMock.discordMessageUpdated).toHaveBeenCalledWith({
      discordMsgId: 'msg-1',
      content: 'Fetched content',
    });
  });
});

describe('messageDelete handler', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('produces discordMessageDeleted for non-bot messages', async () => {
    const client = createClientMock();
    registerMessageDeleteHandler(client as any);

    const message = {
      author: { bot: false },
      id: 'msg-1',
    };

    await client.emit(Events.MessageDelete, message);

    expect(streamProducerMock.discordMessageDeleted).toHaveBeenCalledWith({
      discordMsgId: 'msg-1',
    });
  });

  it('skips bot messages', async () => {
    const client = createClientMock();
    registerMessageDeleteHandler(client as any);

    const message = {
      author: { bot: true },
      id: 'msg-1',
    };

    await client.emit(Events.MessageDelete, message);

    expect(streamProducerMock.discordMessageDeleted).not.toHaveBeenCalled();
  });
});

describe('interactionCreate handler', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('handles invite accept', async () => {
    const client = createClientMock();
    registerInteractionCreateHandler(client as any);

    const interaction = {
      isButton: () => true,
      customId: 'invite_accept_inv-123',
      user: { id: 'user-1' },
      deferUpdate: vi.fn().mockResolvedValue(undefined),
      editReply: vi.fn().mockResolvedValue(undefined),
    };

    await client.emit(Events.InteractionCreate, interaction);

    expect(streamProducerMock.inviteResponse).toHaveBeenCalledWith({
      inviteId: 'inv-123',
      response: 'ACCEPTED',
    });
    expect(interaction.editReply).toHaveBeenCalled();
  });

  it('handles invite decline', async () => {
    const client = createClientMock();
    registerInteractionCreateHandler(client as any);

    const interaction = {
      isButton: () => true,
      customId: 'invite_decline_inv-456',
      user: { id: 'user-2' },
      deferUpdate: vi.fn().mockResolvedValue(undefined),
      editReply: vi.fn().mockResolvedValue(undefined),
    };

    await client.emit(Events.InteractionCreate, interaction);

    expect(streamProducerMock.inviteResponse).toHaveBeenCalledWith({
      inviteId: 'inv-456',
      response: 'DECLINED',
    });
  });

  it('ignores non-button interactions', async () => {
    const client = createClientMock();
    registerInteractionCreateHandler(client as any);

    const interaction = {
      isButton: () => false,
    };

    await client.emit(Events.InteractionCreate, interaction);

    expect(streamProducerMock.inviteResponse).not.toHaveBeenCalled();
  });
});

describe('guildBanAdd handler', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('produces channelDisconnected when bot is banned', async () => {
    const client = createClientMock();
    registerGuildBanAddHandler(client as any);

    const ban = {
      client: { user: { id: 'bot-1' } },
      user: { id: 'bot-1' },
      guild: {
        id: 'guild-1',
        channels: {
          cache: {
            filter: vi.fn(() => new Map([
              ['ch-1', { id: 'ch-1', isTextBased: () => true }],
              ['ch-2', { id: 'ch-2', isTextBased: () => true }],
            ])),
          },
        },
      },
    };

    await client.emit(Events.GuildBanAdd, ban);

    expect(streamProducerMock.channelDisconnected).toHaveBeenCalledTimes(2);
    expect(streamProducerMock.channelDisconnected).toHaveBeenCalledWith({
      channelId: 'ch-1',
      reason: 'BOT_BANNED',
    });
  });

  it('ignores ban of other users', async () => {
    const client = createClientMock();
    registerGuildBanAddHandler(client as any);

    const ban = {
      client: { user: { id: 'bot-1' } },
      user: { id: 'other-user' },
      guild: { channels: { cache: new Map() } },
    };

    await client.emit(Events.GuildBanAdd, ban);

    expect(streamProducerMock.channelDisconnected).not.toHaveBeenCalled();
  });
});

describe('channelDelete handler', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('produces channelDisconnected for guild text channels', async () => {
    const client = createClientMock();
    registerChannelDeleteHandler(client as any);

    const channel = {
      isTextBased: () => true,
      isDMBased: () => false,
      id: 'ch-1',
      guildId: 'guild-1',
    };

    await client.emit(Events.ChannelDelete, channel);

    expect(streamProducerMock.channelDisconnected).toHaveBeenCalledWith({
      channelId: 'ch-1',
      reason: 'CHANNEL_DELETED',
    });
  });

  it('ignores DM channels', async () => {
    const client = createClientMock();
    registerChannelDeleteHandler(client as any);

    const channel = {
      isTextBased: () => true,
      isDMBased: () => true,
      id: 'dm-1',
    };

    await client.emit(Events.ChannelDelete, channel);

    expect(streamProducerMock.channelDisconnected).not.toHaveBeenCalled();
  });

  it('ignores non-text channels', async () => {
    const client = createClientMock();
    registerChannelDeleteHandler(client as any);

    const channel = {
      isTextBased: () => false,
      id: 'vc-1',
    };

    await client.emit(Events.ChannelDelete, channel);

    expect(streamProducerMock.channelDisconnected).not.toHaveBeenCalled();
  });
});
