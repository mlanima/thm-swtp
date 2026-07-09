import { Queue, Worker } from 'bullmq';
import { redis } from '../config/redis.js';
import { logger } from '../config/logger.js';
import {
  handleSendPost,
  handleEditPost,
  handleDeletePost,
  handleSendInvite,
  handleSendEvent,
} from '../streams/eventHandler.js';

export const discordQueue = new Queue('discord-actions', {
  connection: redis as any,
  defaultJobOptions: {
    removeOnComplete: { age: 3600 },
    removeOnFail: { age: 86400 },
  },
});

let worker: Worker | null = null;

export function startDiscordWorker(): Worker {
  worker = new Worker(
    'discord-actions',
    async (job) => {
      switch (job.name) {
        case 'sendPost':
          return handleSendPost(job);
        case 'editPost':
          return handleEditPost(job);
        case 'deletePost':
          logger.info({ discordMsgId: job.data.discordMsgId }, 'deletePost skipped (temporarily disabled)');
          return;
        case 'sendInvite':
          return handleSendInvite(job);
        case 'sendEvent':
          return handleSendEvent(job);
        default:
          logger.warn({ jobName: job.name }, 'unknown job type');
      }
    },
    {
      connection: redis as any,
      concurrency: 5,
      lockDuration: 30_000,
    },
  );

  worker.on('completed', (job) => logger.info({ jobId: job.id, name: job.name }, 'job completed'));
  worker.on('failed', (job, err) => logger.error({ jobId: job?.id, name: job?.name, err }, 'job failed'));

  logger.info('discord worker started');
  return worker;
}

export async function stopDiscordWorker(): Promise<void> {
  if (worker) {
    await worker.close();
    worker = null;
  }
}
