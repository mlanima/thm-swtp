import { logger } from '../config/logger.js';
export async function handleSendPost(job) {
    logger.info({ jobId: job.id, data: job.data }, 'handling sendPost');
}
export async function handleEditPost(job) {
    logger.info({ jobId: job.id, data: job.data }, 'handling editPost');
}
export async function handleDeletePost(job) {
    logger.info({ jobId: job.id, data: job.data }, 'handling deletePost');
}
export async function handleSendInvite(job) {
    logger.info({ jobId: job.id, data: job.data }, 'handling sendInvite');
}
export async function handleSendEvent(job) {
    logger.info({ jobId: job.id, data: job.data }, 'handling sendEvent');
}
//# sourceMappingURL=eventHandler.js.map