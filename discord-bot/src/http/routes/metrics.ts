import type { Router } from 'express';
import { metricsRegistry } from '../../metrics/index.js';

/** Registers GET /metrics — Prometheus-format metrics endpoint. */
export function registerMetricsRoutes(router: Router): void {
  router.get('/metrics', async (_req, res) => {
    res.set('Content-Type', metricsRegistry.contentType);
    res.send(await metricsRegistry.metrics());
  });
}
