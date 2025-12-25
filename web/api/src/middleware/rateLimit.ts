import { Context, Next } from 'hono';
import { Env, ApiResponse } from '../types/env';

interface RateLimitConfig {
  requests: number;      // Max requests
  windowSeconds: number; // Time window in seconds
}

const DEFAULT_CONFIG: RateLimitConfig = {
  requests: 100,
  windowSeconds: 60,
};

/**
 * Rate limiting middleware using KV storage
 * Limits requests per IP address
 */
export function rateLimit(config: Partial<RateLimitConfig> = {}) {
  const { requests, windowSeconds } = { ...DEFAULT_CONFIG, ...config };

  return async (c: Context<{ Bindings: Env }>, next: Next): Promise<Response | void> => {
    const ip = c.req.header('CF-Connecting-IP') ||
               c.req.header('X-Forwarded-For')?.split(',')[0] ||
               'unknown';

    const key = `ratelimit:${ip}`;

    try {
      const current = await c.env.SESSIONS.get(key);
      const count = current ? parseInt(current, 10) : 0;

      if (count >= requests) {
        return c.json<ApiResponse>(
          {
            success: false,
            error: 'Too many requests. Please try again later.'
          },
          429,
          {
            'Retry-After': windowSeconds.toString(),
            'X-RateLimit-Limit': requests.toString(),
            'X-RateLimit-Remaining': '0',
          }
        );
      }

      // Increment counter
      await c.env.SESSIONS.put(key, (count + 1).toString(), {
        expirationTtl: windowSeconds,
      });

      // Add rate limit headers
      c.header('X-RateLimit-Limit', requests.toString());
      c.header('X-RateLimit-Remaining', (requests - count - 1).toString());

      await next();
    } catch (err) {
      // If KV fails, allow request but log error
      console.error('Rate limit error:', err);
      await next();
    }
  };
}

/**
 * Rate limit for auth endpoints
 * Higher limit to accommodate device code polling (every 5 sec = 12 req/min)
 */
export const authRateLimit = rateLimit({
  requests: 60,
  windowSeconds: 60,
});

/**
 * Standard rate limit for API endpoints
 */
export const apiRateLimit = rateLimit({
  requests: 100,
  windowSeconds: 60,
});

/**
 * Stricter rate limit for sync endpoints (device uploads)
 */
export const syncRateLimit = rateLimit({
  requests: 30,
  windowSeconds: 60,
});
