import { logger } from '../config/logger.js';
import type { Job } from 'bullmq';

export async function handleSendPost(job: Job): Promise<void> {
  logger.info({ jobId: job.id, data: job.data }, 'handling sendPost');
}

export async function handleEditPost(job: Job): Promise<void> {
  logger.info({ jobId: job.id, data: job.data }, 'handling editPost');
}

export async function handleDeletePost(job: Job): Promise<void> {
  logger.info({ jobId: job.id, data: job.data }, 'handling deletePost');
}

export async function handleSendInvite(job: Job): Promise<void> {
  logger.info({ jobId: job.id, data: job.data }, 'handling sendInvite');
}

export async function handleSendEvent(job: Job): Promise<void> {
  logger.info({ jobId: job.id, data: job.data }, 'handling sendEvent');
}
