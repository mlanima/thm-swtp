import { Registry, Counter, Gauge } from 'prom-client';
export declare const metricsRegistry: Registry<"text/plain; version=0.0.4; charset=utf-8">;
export declare const eventsProcessed: Counter<"event_type">;
export declare const messagesSent: Counter<"status">;
export declare const redisStreamLag: Gauge<string>;
export declare const queueDepth: Gauge<string>;
//# sourceMappingURL=index.d.ts.map