import express from 'express';
import { logger } from '../config/logger.js';
import { pingRedis } from '../config/redis.js';
import { discordClient, getDiscordStatus } from '../bot/client.js';
import { metricsRegistry } from '../metrics/index.js';
import { discordQueue } from '../queues/discordQueue.js';
export const internalApi = express();
internalApi.use(express.json());
function validateSecret(req, res, next) {
    const secret = req.headers['x-internal-secret'];
    if (secret !== process.env.PLATFORM_API_SECRET) {
        res.status(401).json({ error: 'unauthorized' });
        return;
    }
    next();
}
internalApi.get('/health', async (_req, res) => {
    const redis = await pingRedis();
    const discord = getDiscordStatus();
    const status = redis && discord === 'ready' ? 'ok' : 'degraded';
    res.json({ status, discord, redis: redis ? 'up' : 'down' });
});
internalApi.get('/metrics', async (_req, res) => {
    res.set('Content-Type', metricsRegistry.contentType);
    res.send(await metricsRegistry.metrics());
});
internalApi.post('/internal/test-connection', validateSecret, async (req, res) => {
    const { channelId } = req.body;
    if (!channelId) {
        res.status(400).json({ success: false, reason: 'channelId is required' });
        return;
    }
    try {
        const channel = await discordClient.channels.fetch(channelId);
        if (!channel?.isTextBased()) {
            res.json({ success: false, reason: 'channel not found or not a text channel' });
            return;
        }
        res.json({ success: true });
    }
    catch (err) {
        logger.error({ err, channelId }, 'test-connection failed');
        res.json({ success: false, reason: 'bot cannot access this channel' });
    }
});
internalApi.post('/internal/jobs/:jobId/retry', validateSecret, async (req, res) => {
    const jobId = req.params.jobId;
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
    }
    catch (err) {
        logger.error({ err, jobId }, 'retry failed');
        res.json({ success: false, reason: 'retry failed' });
    }
});
export function startInternalApi(port) {
    internalApi.listen(port, () => {
        logger.info({ port }, 'internal API listening');
    });
}
//# sourceMappingURL=internalApi.js.map