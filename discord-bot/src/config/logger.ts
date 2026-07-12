import pino from 'pino';

/** Application-wide pino logger. Log level from `LOG_LEVEL` env var (default: `info`). */
export const logger = pino({
  level: process.env.LOG_LEVEL ?? 'info',
  // Pretty-print to stdout in dev, structured JSON in production
  transport:
    process.env.NODE_ENV !== 'production'
      ? { target: 'pino/file', options: { destination: 1 } }
      : undefined,
});
