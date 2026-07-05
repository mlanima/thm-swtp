import { Registry, Counter, Gauge } from 'prom-client';
export const metricsRegistry = new Registry();
export const eventsProcessed = new Counter({
    name: 'discord_events_processed_total',
    help: 'Total events processed from Redis stream',
    labelNames: ['event_type'],
    registers: [metricsRegistry],
});
export const messagesSent = new Counter({
    name: 'discord_messages_sent_total',
    help: 'Total Discord messages sent',
    labelNames: ['status'],
    registers: [metricsRegistry],
});
export const redisStreamLag = new Gauge({
    name: 'discord_redis_stream_lag',
    help: 'Lag between last produced and last consumed message',
    registers: [metricsRegistry],
});
export const queueDepth = new Gauge({
    name: 'discord_queue_depth',
    help: 'Number of pending jobs in BullMQ queue',
    registers: [metricsRegistry],
});
//# sourceMappingURL=index.js.map