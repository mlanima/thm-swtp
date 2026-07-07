import { logger } from '../config/logger.js';

export function wrapAsync<T extends (...args: any[]) => void>(
  fn: (...args: Parameters<T>) => Promise<void>,
  label: string,
): (...args: Parameters<T>) => void {
  return (...args: Parameters<T>) => {
    fn(...args).catch((err) => {
      logger.error({ err, handler: label }, 'unhandled rejection in event handler');
    });
  };
}
