import { logger } from '../config/logger.js';

/**
 * Wraps an async event handler so discord.js never gets an unhandled rejection.
 * Logs errors with a label instead of crashing the process.
 */
export function wrapAsync<T extends (...args: any[]) => void>(
  fn: (...args: Parameters<T>) => Promise<void>,
  label: string,
): (...args: Parameters<T>) => void {
  return (...args: Parameters<T>) => {
    // Fire-and-forget: discord.js expects a void return, errors go to the logger
    fn(...args).catch((err) => {
      logger.error({ err, handler: label }, 'unhandled rejection in event handler');
    });
  };
}
