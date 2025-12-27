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

// In-memory cache for rate limit state (lives as long as Worker instance)
// Key: IP, Value: { count, blocked, lastReset }
const rateLimitCache = new Map<string, { count: number; blocked: boolean; resetAt: number }>();

/**
 * Optimistic rate limiting middleware
 *
 * Instead of blocking on KV read/write for every request (~100ms),
 * uses in-memory cache + background KV sync:
 * - First layer: In-memory cache (0ms) - instant decision
 * - Second layer: KV storage (background) - persistent across Worker instances
 *
 * This reduces latency from ~100ms to ~0ms for most requests.
 * Trade-off: Rate limits may be slightly exceeded across Worker instances.
 */
export function rateLimit(config: Partial<RateLimitConfig> = {}) {
  const { requests, windowSeconds } = { ...DEFAULT_CONFIG, ...config };
  const windowMs = windowSeconds * 1000;

  return async (c: Context<{ Bindings: Env }>, next: Next): Promise<Response | void> => {
    const ip = c.req.header('CF-Connecting-IP') ||
               c.req.header('X-Forwarded-For')?.split(',')[0] ||
               'unknown';

    const now = Date.now();
    const cacheKey = `${ip}:${requests}`; // Include limit in key for different routes
    const kvKey = `ratelimit:${ip}:${requests}`;

    // Check in-memory cache first (0ms)
    let cached = rateLimitCache.get(cacheKey);

    // Reset if window expired
    if (cached && now >= cached.resetAt) {
      cached = undefined;
      rateLimitCache.delete(cacheKey);
    }

    // If blocked in cache, reject immediately
    if (cached?.blocked) {
      return c.json<ApiResponse>(
        { success: false, error: 'Too many requests. Please try again later.' },
        429,
        {
          'Retry-After': Math.ceil((cached.resetAt - now) / 1000).toString(),
          'X-RateLimit-Limit': requests.toString(),
          'X-RateLimit-Remaining': '0',
        }
      );
    }

    // Initialize or increment in-memory counter
    if (!cached) {
      cached = { count: 1, blocked: false, resetAt: now + windowMs };
      rateLimitCache.set(cacheKey, cached);

      // Cleanup old entries when cache grows too large (lazy cleanup)
      if (rateLimitCache.size > 1000) {
        cleanupCache();
      }
    } else {
      cached.count++;
    }

    // Check if limit exceeded
    if (cached.count > requests) {
      cached.blocked = true;
      return c.json<ApiResponse>(
        { success: false, error: 'Too many requests. Please try again later.' },
        429,
        {
          'Retry-After': windowSeconds.toString(),
          'X-RateLimit-Limit': requests.toString(),
          'X-RateLimit-Remaining': '0',
        }
      );
    }

    // Add rate limit headers
    c.header('X-RateLimit-Limit', requests.toString());
    c.header('X-RateLimit-Remaining', Math.max(0, requests - cached.count).toString());

    // Background: sync with KV for persistence across Worker instances
    // Only sync every 10 requests to reduce KV writes
    if (cached.count % 10 === 0) {
      c.executionCtx?.waitUntil(
        syncRateLimitToKV(c.env.SESSIONS, kvKey, cached.count, windowSeconds).catch(() => {})
      );
    }

    await next();
  };
}

/**
 * Sync rate limit counter to KV (background)
 * Only persists the count, not the full state
 */
async function syncRateLimitToKV(
  kv: KVNamespace,
  key: string,
  count: number,
  ttlSeconds: number
): Promise<void> {
  const existing = await kv.get(key);
  const existingCount = existing ? parseInt(existing, 10) : 0;

  // Only update if our count is higher (other instances may have higher)
  if (count > existingCount) {
    await kv.put(key, count.toString(), { expirationTtl: ttlSeconds });
  }
}

// Clean up old cache entries when cache gets too large
function cleanupCache(): void {
  const now = Date.now();
  for (const [key, value] of rateLimitCache) {
    if (now >= value.resetAt) {
      rateLimitCache.delete(key);
    }
  }
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
