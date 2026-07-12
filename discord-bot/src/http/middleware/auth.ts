import type { Request, Response, NextFunction } from 'express';

/** Express middleware — rejects requests that don't carry a valid `x-internal-secret` header. */
export function validateSecret(req: Request, res: Response, next: NextFunction): void {
  const secret = req.headers['x-internal-secret'] as string | undefined;
  const expected = process.env.PLATFORM_API_SECRET;
  if (!secret || !expected || secret !== expected) {
    res.status(401).json({ error: 'unauthorized' });
    return;
  }
  next();
}
