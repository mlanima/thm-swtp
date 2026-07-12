import { Queue, Worker, type Job } from 'bullmq';
import { redis, redisConnection } from '../config/redis.js';
import { logger } from '../config/logger.js';
import { eventRegistry } from '../events/eventRegistry.js';
import { eventHandlers } from '../streams/eventHandler.js';

/** Maps job names (from event registry) to their handler functions. */
const handlerMap: Record<string, (job: Job) => Promise<void>> = {
  sendPost: eventHandlers.handleSendPost,
  editPost: eventHandlers.handleEditPost,
  deletePost: eventHandlers.handleDeletePost,
  sendInvite: eventHandlers.handleSendInvite,
  sendEvent: eventHandlers.handleSendEvent,
};

// Warn if a handler is registered but has no corresponding event-registry entry
const registeredJobNames: Set<string> = new Set(Object.values(eventRegistry).map((c) => c.jobName));
for (const name of Object.keys(handlerMap)) {
  if (!registeredJobNames.has(name)) {
    logger.warn({ jobName: name }, 'handler registered but not in event registry');
  }
}

/** BullMQ queue for Discord actions (send, edit, delete posts, invites, events). */
export const discordQueue = new Queue('discord-actions', {
  connection: redisConnection,
  defaultJobOptions: {
    removeOnComplete: { age: 3600 },
    removeOnFail: { age: 86400 },
  },
});

let worker: Worker | null = null;

/** Starts the BullMQ worker that processes Discord-action jobs. */
export function startDiscordWorker(): Worker {
  worker = new Worker(
    'discord-actions',
    async (job) => {
      const handler = handlerMap[job.name];
      if (handler) {
        return handler(job);
      }
      logger.warn({ jobName: job.name }, 'unknown job type');
    },
    {
      connection: redisConnection,
      concurrency: 5,
      lockDuration: 30_000,
    },
  );

  worker.on('completed', (job) => logger.info({ jobId: job.id, name: job.name }, 'job completed'));
  worker.on('failed', (job, err) => logger.error({ jobId: job?.id, name: job?.name, err }, 'job failed'));

  logger.info('discord worker started');
  return worker;
}

/** Gracefully stops the BullMQ worker. */
export async function stopDiscordWorker(): Promise<void> {
  if (worker) {
    await worker.close();
    worker = null;
  }
}
