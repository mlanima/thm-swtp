import type { Router } from 'express';
import { logger } from '../../config/logger.js';
import { discordQueue } from '../../queues/discordQueue.js';
import { validateSecret } from '../middleware/auth.js';

/** Registers POST /internal/jobs/:jobId/retry — retries a failed BullMQ job. */
export function registerJobRoutes(router: Router): void {
  router.post('/internal/jobs/:jobId/retry', validateSecret, async (req, res) => {
    const jobId = req.params.jobId as string;
    if (!jobId) {
      res.status(400).json({ success: false, reason: 'jobId is required' });
      return;
    }
    try {
      const job = await discordQueue.getJob(jobId);
      if (!job) {
        res.status(404).json({ success: false, reason: 'job not found' });
        return;
      }
      await job.retry();
      res.json({ success: true });
    } catch (err) {
      logger.error({ err, jobId }, 'retry failed');
      res.json({ success: false, reason: 'retry failed' });
    }
  });
}
