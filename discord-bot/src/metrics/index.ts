import { Registry, Counter, Gauge } from 'prom-client';

/** Shared Prometheus registry for all bot metrics. */
export const metricsRegistry = new Registry();

/** Counts stream events processed by type (POST_CREATED, POST_UPDATED, etc.). */
export const eventsProcessed = new Counter({
  name: 'discord_events_processed_total',
  help: 'Total events processed from Redis stream',
  labelNames: ['event_type'] as const,
  registers: [metricsRegistry],
});

/** Counts Discord messages sent, labelled by status (success, edit, dm, event). */
export const messagesSent = new Counter({
  name: 'discord_messages_sent_total',
  help: 'Total Discord messages sent',
  labelNames: ['status'] as const,
  registers: [metricsRegistry],
});

/** Tracks how far behind the consumer is from the latest stream entry. */
export const redisStreamLag = new Gauge({
  name: 'discord_redis_stream_lag',
  help: 'Lag between last produced and last consumed message',
  registers: [metricsRegistry],
});

/** Tracks the number of pending jobs in the BullMQ queue. */
export const queueDepth = new Gauge({
  name: 'discord_queue_depth',
  help: 'Number of pending jobs in BullMQ queue',
  registers: [metricsRegistry],
});
