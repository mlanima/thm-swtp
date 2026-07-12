import { describe, it, expect, vi, beforeEach } from 'vitest';
import { createConsumer } from '../streams/consumer.js';
import { eventRegistry } from '../events/eventRegistry.js';
import type { Redis } from 'ioredis';
import type { Queue } from 'bullmq';
import type { Logger } from 'pino';

function mockRedis(): Redis {
  return {
    xreadgroup: vi.fn(),
    xack: vi.fn().mockResolvedValue(1),
    xgroup: vi.fn().mockResolvedValue('OK'),
  } as unknown as Redis;
}

function mockQueue(): Queue {
  return {
    add: vi.fn().mockResolvedValue({ id: 'job-1' } as any),
  } as unknown as Queue;
}

function mockLogger(): Logger {
  return {
    info: vi.fn(),
    warn: vi.fn(),
    error: vi.fn(),
    debug: vi.fn(),
    fatal: vi.fn(),
  } as unknown as Logger;
}

function makeStreamResult(type: string, payload: Record<string, unknown>, messageId = 'msg-1') {
  return [[
    'stream:discord:sync',
    [[messageId, ['type', type, 'payload', JSON.stringify(payload)]]],
  ]] as unknown as Array<[string, Array<[string, string[]]>]>;
}

describe('createConsumer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('does nothing when no messages in stream', async () => {
    const redis = mockRedis();
    const queue = mockQueue();
    const logger = mockLogger();
    vi.mocked(redis.xreadgroup).mockResolvedValue(null as unknown as Array<[string, Array<[string, string[]]>]>);

    const { processBatch } = createConsumer({ redis, logger, queue, registry: eventRegistry });

    await processBatch();

    expect(queue.add).not.toHaveBeenCalled();
  });

  it('enqueues a job and acks for valid POST_CREATED', async () => {
    const redis = mockRedis();
    const queue = mockQueue();
    const logger = mockLogger();
    vi.mocked(redis.xreadgroup).mockResolvedValue(
      makeStreamResult('POST_CREATED', {
        postId: '550e8400-e29b-41d4-a716-446655440000',
        projectId: '660e8400-e29b-41d4-a716-446655440001',
        channelId: '123456789012345678',
        content: 'Hello',
        title: 'Test',
        authorName: 'Alice',
        platformUrl: 'https://example.com',
      })
    );

    const { processBatch } = createConsumer({ redis, logger, queue, registry: eventRegistry });

    await processBatch();

    expect(queue.add).toHaveBeenCalledWith('sendPost', expect.any(Object), expect.any(Object));
    expect(redis.xack).toHaveBeenCalledWith('stream:discord:sync', 'discord-bot', 'msg-1');
  });

  it('acks and skips unknown event type', async () => {
    const redis = mockRedis();
    const queue = mockQueue();
    const logger = mockLogger();
    vi.mocked(redis.xreadgroup).mockResolvedValue(
      makeStreamResult('UNKNOWN_EVENT', {})
    );

    const { processBatch } = createConsumer({ redis, logger, queue, registry: eventRegistry });

    await processBatch();

    expect(queue.add).not.toHaveBeenCalled();
    expect(redis.xack).toHaveBeenCalledWith('stream:discord:sync', 'discord-bot', 'msg-1');
    expect(logger.warn).toHaveBeenCalled();
  });

  it('acks and skips invalid payload (ZodError)', async () => {
    const redis = mockRedis();
    const queue = mockQueue();
    const logger = mockLogger();
    vi.mocked(redis.xreadgroup).mockResolvedValue(
      makeStreamResult('POST_CREATED', { invalid: true })
    );

    const { processBatch } = createConsumer({ redis, logger, queue, registry: eventRegistry });

    await processBatch();

    expect(queue.add).not.toHaveBeenCalled();
    expect(redis.xack).toHaveBeenCalled();
  });

  it('processes multiple messages in one batch', async () => {
    const redis = mockRedis();
    const queue = mockQueue();
    const logger = mockLogger();
    const validPayload = {
      postId: '550e8400-e29b-41d4-a716-446655440000',
      projectId: '660e8400-e29b-41d4-a716-446655440001',
      channelId: '123456789012345678',
      content: 'Hello',
      title: 'Test',
      authorName: 'Alice',
      platformUrl: 'https://example.com',
    };
    vi.mocked(redis.xreadgroup).mockResolvedValue([
      ['stream:discord:sync', [
        ['msg-1', ['type', 'POST_CREATED', 'payload', JSON.stringify(validPayload)]],
        ['msg-2', ['type', 'PROJECT_INVITE', 'payload', JSON.stringify({
          inviteId: '550e8400-e29b-41d4-a716-446655440000',
          targetDiscordId: 'user-1',
          projectName: 'Test',
          inviterName: 'Alice',
        })]],
      ]],
    ] as any);

    const { processBatch } = createConsumer({ redis, logger, queue, registry: eventRegistry });

    await processBatch();

    expect(queue.add).toHaveBeenCalledTimes(2);
    expect(queue.add).toHaveBeenCalledWith('sendPost', expect.any(Object), expect.any(Object));
    expect(queue.add).toHaveBeenCalledWith('sendInvite', expect.any(Object), expect.any(Object));
    expect(redis.xack).toHaveBeenCalledTimes(2);
  });

  it('handles NOGROUP error by calling ensureGroup', async () => {
    const redis = mockRedis();
    const queue = mockQueue();
    const logger = mockLogger();
    const xgroupError = new Error('NOGROUP No such consumer group');
    vi.mocked(redis.xreadgroup).mockRejectedValue(xgroupError);
    vi.mocked(redis.xgroup).mockResolvedValue('OK');

    const { processBatch } = createConsumer({ redis, logger, queue, registry: eventRegistry });

    await processBatch();

    expect(redis.xgroup).toHaveBeenCalled();
    expect(logger.error).toHaveBeenCalled();
  });

  it('uses custom stream key and group when provided', async () => {
    const redis = mockRedis();
    const queue = mockQueue();
    const logger = mockLogger();
    vi.mocked(redis.xreadgroup).mockResolvedValue(null as unknown as Array<[string, Array<[string, string[]]>]>);

    const { processBatch } = createConsumer({
      redis, logger, queue, registry: eventRegistry,
      streamKey: 'custom:stream', consumerGroup: 'custom-group', consumerName: 'test-worker',
    });

    await processBatch();

    expect(redis.xreadgroup).toHaveBeenCalledWith(
      'GROUP', 'custom-group', 'test-worker',
      'COUNT', 10, 'BLOCK', 5000,
      'STREAMS', 'custom:stream', '>',
    );
  });
});
