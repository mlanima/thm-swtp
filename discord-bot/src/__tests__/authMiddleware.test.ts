import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import type { Request, Response, NextFunction } from 'express';

// Set env before importing the module under test
const ORIGINAL_SECRET = process.env.PLATFORM_API_SECRET;

describe('validateSecret', () => {
  let validateSecret: (req: Request, res: Response, next: NextFunction) => void;

  function mockReq(headers: Record<string, string | undefined>): Request {
    return { headers } as unknown as Request;
  }

  function mockRes(): Response {
    const res: Partial<Response> = {};
    res.status = vi.fn().mockReturnValue(res);
    res.json = vi.fn().mockReturnValue(res);
    return res as Response;
  }

  beforeEach(() => {
    vi.resetModules();
  });

  afterEach(() => {
    process.env.PLATFORM_API_SECRET = ORIGINAL_SECRET;
  });

  it('calls next when secret matches', async () => {
    process.env.PLATFORM_API_SECRET = 'my-secret';
    const mod = await import('../http/middleware/auth.js');
    validateSecret = mod.validateSecret;

    const req = mockReq({ 'x-internal-secret': 'my-secret' });
    const res = mockRes();
    const next = vi.fn();

    validateSecret(req, res, next);

    expect(next).toHaveBeenCalledOnce();
    expect(res.status).not.toHaveBeenCalled();
  });

  it('returns 401 when secret is missing', async () => {
    process.env.PLATFORM_API_SECRET = 'my-secret';
    const mod = await import('../http/middleware/auth.js');
    validateSecret = mod.validateSecret;

    const req = mockReq({});
    const res = mockRes();
    const next = vi.fn();

    validateSecret(req, res, next);

    expect(res.status).toHaveBeenCalledWith(401);
    expect(res.json).toHaveBeenCalledWith({ error: 'unauthorized' });
    expect(next).not.toHaveBeenCalled();
  });

  it('returns 401 when secret is wrong', async () => {
    process.env.PLATFORM_API_SECRET = 'my-secret';
    const mod = await import('../http/middleware/auth.js');
    validateSecret = mod.validateSecret;

    const req = mockReq({ 'x-internal-secret': 'wrong-secret' });
    const res = mockRes();
    const next = vi.fn();

    validateSecret(req, res, next);

    expect(res.status).toHaveBeenCalledWith(401);
    expect(next).not.toHaveBeenCalled();
  });

  it('returns 401 when expected secret is not set', async () => {
    delete process.env.PLATFORM_API_SECRET;
    const mod = await import('../http/middleware/auth.js');
    validateSecret = mod.validateSecret;

    const req = mockReq({ 'x-internal-secret': 'anything' });
    const res = mockRes();
    const next = vi.fn();

    validateSecret(req, res, next);

    expect(res.status).toHaveBeenCalledWith(401);
    expect(next).not.toHaveBeenCalled();
  });
});
