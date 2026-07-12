import { describe, it, expect, vi, beforeEach } from 'vitest';
import request from 'supertest';

vi.mock('../config/redis.js', () => ({
  pingRedis: vi.fn().mockResolvedValue(true),
}));

vi.mock('../bot/client.js', () => ({
  discordClient: {
    guilds: {
      cache: {
        size: 0,
        first: vi.fn(() => undefined),
        get: vi.fn(() => undefined),
        find: vi.fn(() => undefined),
        map: vi.fn(() => []),
      },
      fetch: vi.fn(),
    },
    channels: { fetch: vi.fn() },
    rest: { put: vi.fn() },
  },
  getDiscordStatus: vi.fn().mockReturnValue('ready'),
}));

vi.mock('../metrics/index.js', () => ({
  metricsRegistry: {
    contentType: 'text/plain',
    metrics: vi.fn().mockResolvedValue('mock metrics output'),
  },
}));

vi.mock('../queues/discordQueue.js', () => ({
  discordQueue: {
    getJob: vi.fn(),
  },
}));

process.env.PLATFORM_API_SECRET = 'test-secret';

const { app } = await import('../http/server.js');

describe('health routes', () => {
  it('GET /health returns ok when redis and discord are up', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('ok');
  });
});

describe('metrics routes', () => {
  it('GET /metrics returns prometheus metrics', async () => {
    const res = await request(app).get('/metrics');
    expect(res.status).toBe(200);
    expect(res.text).toContain('mock metrics output');
  });
});

describe('guild routes', () => {
  it('GET /internal/guilds returns guilds list', async () => {
    const res = await request(app).get('/internal/guilds').set('x-internal-secret', 'test-secret');
    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
  });

  it('returns 401 without secret', async () => {
    const res = await request(app).get('/internal/guilds');
    expect(res.status).toBe(401);
  });
});

describe('channel routes', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('POST /internal/channels/:id/leave-guild returns error for unknown channel', async () => {
    const { discordClient } = await import('../bot/client.js');
    vi.mocked(discordClient.channels.fetch).mockResolvedValue(null);

    const res = await request(app)
      .post('/internal/channels/ch-999/leave-guild')
      .set('x-internal-secret', 'test-secret');
    expect(res.body.success).toBe(false);
  });

  it('POST /internal/channels/:id/restrict returns 400 without ownerDiscordId', async () => {
    const res = await request(app)
      .post('/internal/channels/ch-1/restrict')
      .send({})
      .set('x-internal-secret', 'test-secret');
    expect(res.status).toBe(400);
  });

  it('POST /internal/channels/:id/invite returns error for unknown channel', async () => {
    const { discordClient } = await import('../bot/client.js');
    vi.mocked(discordClient.channels.fetch).mockResolvedValue(null);

    const res = await request(app)
      .post('/internal/channels/ch-999/invite')
      .set('x-internal-secret', 'test-secret');
    expect(res.body.success).toBe(false);
  });

  it('POST /internal/test-connection returns 400 without channelId', async () => {
    const res = await request(app)
      .post('/internal/test-connection')
      .send({})
      .set('x-internal-secret', 'test-secret');
    expect(res.status).toBe(400);
  });

  it('POST /internal/test-connection succeeds for valid channel', async () => {
    const { discordClient } = await import('../bot/client.js');
    vi.mocked(discordClient.channels.fetch).mockResolvedValue({
      isTextBased: () => true,
    } as any);

    const res = await request(app)
      .post('/internal/test-connection')
      .send({ channelId: 'ch-1' })
      .set('x-internal-secret', 'test-secret');
    expect(res.body.success).toBe(true);
  });
});

describe('auto-setup routes', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('POST /internal/auto-setup returns error when bot is not in any guild', async () => {
    const res = await request(app)
      .post('/internal/auto-setup')
      .send({})
      .set('x-internal-secret', 'test-secret');
    expect(res.body.success).toBe(false);
    expect(res.body.reason).toContain('not in any guild');
  });

  it('POST /internal/auto-setup returns error when guild not found', async () => {
    const { discordClient } = await import('../bot/client.js');
    (discordClient.guilds as any).cache = new Map();
    (discordClient.guilds as any).fetch = vi.fn().mockRejectedValue(new Error('not found'));

    const res = await request(app)
      .post('/internal/auto-setup')
      .send({ guildId: 'guild-999' })
      .set('x-internal-secret', 'test-secret');
    expect(res.body.success).toBe(false);
  });
});

describe('job routes', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('POST /internal/jobs/:id/retry returns 404 when job not found', async () => {
    const { discordQueue } = await import('../queues/discordQueue.js');
    vi.mocked(discordQueue.getJob).mockResolvedValue(undefined as any);

    const res = await request(app)
      .post('/internal/jobs/job-1/retry')
      .set('x-internal-secret', 'test-secret');
    expect(res.status).toBe(404);
  });

  it('POST /internal/jobs/:id/retry returns success when job is retried', async () => {
    const { discordQueue } = await import('../queues/discordQueue.js');
    vi.mocked(discordQueue.getJob).mockResolvedValue({ retry: vi.fn().mockResolvedValue(undefined) } as any);

    const res = await request(app)
      .post('/internal/jobs/job-1/retry')
      .set('x-internal-secret', 'test-secret');
    expect(res.body.success).toBe(true);
  });
});
