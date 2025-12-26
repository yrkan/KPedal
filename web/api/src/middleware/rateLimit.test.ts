import { describe, it, expect, beforeEach } from 'vitest';

/**
 * Unit tests for rate limiting middleware
 *
 * Tests rate limit enforcement, headers, and configuration
 */

// Mock KV store for rate limiting
class MockRateLimitKV {
  private store: Map<string, { value: string; expiration?: number }> = new Map();

  async get(key: string): Promise<string | null> {
    const entry = this.store.get(key);
    if (!entry) return null;
    if (entry.expiration && Date.now() > entry.expiration) {
      this.store.delete(key);
      return null;
    }
    return entry.value;
  }

  async put(key: string, value: string, options?: { expirationTtl?: number }): Promise<void> {
    const expiration = options?.expirationTtl
      ? Date.now() + options.expirationTtl * 1000
      : undefined;
    this.store.set(key, { value, expiration });
  }

  async delete(key: string): Promise<void> {
    this.store.delete(key);
  }

  clear(): void {
    this.store.clear();
  }

  // Helper to simulate time passing
  expireAll(): void {
    this.store.clear();
  }
}

interface RateLimitConfig {
  requests: number;
  windowSeconds: number;
}

interface RateLimitResult {
  allowed: boolean;
  status?: number;
  headers: Record<string, string>;
  error?: string;
}

// Rate limit logic (mirrors rateLimit.ts)
async function checkRateLimit(
  kv: MockRateLimitKV,
  ip: string,
  config: RateLimitConfig
): Promise<RateLimitResult> {
  const { requests, windowSeconds } = config;
  const key = `ratelimit:${ip}`;

  const current = await kv.get(key);
  const count = current ? parseInt(current, 10) : 0;

  if (count >= requests) {
    return {
      allowed: false,
      status: 429,
      headers: {
        'Retry-After': windowSeconds.toString(),
        'X-RateLimit-Limit': requests.toString(),
        'X-RateLimit-Remaining': '0',
      },
      error: 'Too many requests. Please try again later.',
    };
  }

  await kv.put(key, (count + 1).toString(), {
    expirationTtl: windowSeconds,
  });

  return {
    allowed: true,
    headers: {
      'X-RateLimit-Limit': requests.toString(),
      'X-RateLimit-Remaining': (requests - count - 1).toString(),
    },
  };
}

describe('Rate Limit - Basic Functionality', () => {
  let kv: MockRateLimitKV;

  beforeEach(() => {
    kv = new MockRateLimitKV();
  });

  it('should allow first request', async () => {
    const result = await checkRateLimit(kv, '192.168.1.1', {
      requests: 10,
      windowSeconds: 60,
    });

    expect(result.allowed).toBe(true);
    expect(result.status).toBeUndefined();
  });

  it('should allow requests up to the limit', async () => {
    const config = { requests: 5, windowSeconds: 60 };
    const ip = '192.168.1.1';

    for (let i = 0; i < 5; i++) {
      const result = await checkRateLimit(kv, ip, config);
      expect(result.allowed).toBe(true);
    }
  });

  it('should block request when limit is exceeded', async () => {
    const config = { requests: 3, windowSeconds: 60 };
    const ip = '192.168.1.1';

    // Make 3 allowed requests
    await checkRateLimit(kv, ip, config);
    await checkRateLimit(kv, ip, config);
    await checkRateLimit(kv, ip, config);

    // 4th request should be blocked
    const result = await checkRateLimit(kv, ip, config);

    expect(result.allowed).toBe(false);
    expect(result.status).toBe(429);
    expect(result.error).toBe('Too many requests. Please try again later.');
  });

  it('should track requests per IP independently', async () => {
    const config = { requests: 2, windowSeconds: 60 };

    // IP 1: 2 requests (at limit)
    await checkRateLimit(kv, '192.168.1.1', config);
    await checkRateLimit(kv, '192.168.1.1', config);

    // IP 1 should be blocked
    const ip1Result = await checkRateLimit(kv, '192.168.1.1', config);
    expect(ip1Result.allowed).toBe(false);

    // IP 2 should still be allowed
    const ip2Result = await checkRateLimit(kv, '192.168.1.2', config);
    expect(ip2Result.allowed).toBe(true);
  });
});

describe('Rate Limit - Headers', () => {
  let kv: MockRateLimitKV;

  beforeEach(() => {
    kv = new MockRateLimitKV();
  });

  it('should include X-RateLimit-Limit header', async () => {
    const result = await checkRateLimit(kv, '192.168.1.1', {
      requests: 100,
      windowSeconds: 60,
    });

    expect(result.headers['X-RateLimit-Limit']).toBe('100');
  });

  it('should include X-RateLimit-Remaining header', async () => {
    const config = { requests: 10, windowSeconds: 60 };
    const ip = '192.168.1.1';

    const result1 = await checkRateLimit(kv, ip, config);
    expect(result1.headers['X-RateLimit-Remaining']).toBe('9');

    const result2 = await checkRateLimit(kv, ip, config);
    expect(result2.headers['X-RateLimit-Remaining']).toBe('8');

    const result3 = await checkRateLimit(kv, ip, config);
    expect(result3.headers['X-RateLimit-Remaining']).toBe('7');
  });

  it('should show 0 remaining when blocked', async () => {
    const config = { requests: 1, windowSeconds: 60 };
    const ip = '192.168.1.1';

    await checkRateLimit(kv, ip, config);
    const result = await checkRateLimit(kv, ip, config);

    expect(result.headers['X-RateLimit-Remaining']).toBe('0');
  });

  it('should include Retry-After header when blocked', async () => {
    const config = { requests: 1, windowSeconds: 120 };
    const ip = '192.168.1.1';

    await checkRateLimit(kv, ip, config);
    const result = await checkRateLimit(kv, ip, config);

    expect(result.headers['Retry-After']).toBe('120');
  });
});

describe('Rate Limit - Configuration', () => {
  let kv: MockRateLimitKV;

  beforeEach(() => {
    kv = new MockRateLimitKV();
  });

  it('should respect custom request limit', async () => {
    const ip = '192.168.1.1';

    // Config with limit of 2
    const config = { requests: 2, windowSeconds: 60 };

    const result1 = await checkRateLimit(kv, ip, config);
    expect(result1.allowed).toBe(true);

    const result2 = await checkRateLimit(kv, ip, config);
    expect(result2.allowed).toBe(true);

    const result3 = await checkRateLimit(kv, ip, config);
    expect(result3.allowed).toBe(false);
  });

  it('should handle very high limits', async () => {
    const config = { requests: 10000, windowSeconds: 60 };

    const result = await checkRateLimit(kv, '192.168.1.1', config);

    expect(result.allowed).toBe(true);
    expect(result.headers['X-RateLimit-Limit']).toBe('10000');
    expect(result.headers['X-RateLimit-Remaining']).toBe('9999');
  });

  it('should handle limit of 1', async () => {
    const config = { requests: 1, windowSeconds: 60 };
    const ip = '192.168.1.1';

    const result1 = await checkRateLimit(kv, ip, config);
    expect(result1.allowed).toBe(true);
    expect(result1.headers['X-RateLimit-Remaining']).toBe('0');

    const result2 = await checkRateLimit(kv, ip, config);
    expect(result2.allowed).toBe(false);
  });
});

describe('Rate Limit - Window Expiration', () => {
  let kv: MockRateLimitKV;

  beforeEach(() => {
    kv = new MockRateLimitKV();
  });

  it('should reset counter after window expires', async () => {
    const config = { requests: 1, windowSeconds: 60 };
    const ip = '192.168.1.1';

    // First request uses up the limit
    await checkRateLimit(kv, ip, config);

    // Should be blocked
    const blocked = await checkRateLimit(kv, ip, config);
    expect(blocked.allowed).toBe(false);

    // Simulate window expiration
    kv.expireAll();

    // Should be allowed again
    const allowed = await checkRateLimit(kv, ip, config);
    expect(allowed.allowed).toBe(true);
  });
});

describe('Rate Limit - Preset Configurations', () => {
  // These match the presets in rateLimit.ts
  const AUTH_RATE_LIMIT = { requests: 60, windowSeconds: 60 };
  const API_RATE_LIMIT = { requests: 100, windowSeconds: 60 };
  const SYNC_RATE_LIMIT = { requests: 30, windowSeconds: 60 };

  let kv: MockRateLimitKV;

  beforeEach(() => {
    kv = new MockRateLimitKV();
  });

  it('authRateLimit should allow 60 requests per minute', async () => {
    const ip = '192.168.1.1';

    // Make 60 requests
    for (let i = 0; i < 60; i++) {
      const result = await checkRateLimit(kv, ip, AUTH_RATE_LIMIT);
      expect(result.allowed).toBe(true);
    }

    // 61st should be blocked
    const result = await checkRateLimit(kv, ip, AUTH_RATE_LIMIT);
    expect(result.allowed).toBe(false);
  });

  it('apiRateLimit should allow 100 requests per minute', async () => {
    const result = await checkRateLimit(kv, '192.168.1.1', API_RATE_LIMIT);

    expect(result.headers['X-RateLimit-Limit']).toBe('100');
  });

  it('syncRateLimit should allow 30 requests per minute', async () => {
    const result = await checkRateLimit(kv, '192.168.1.1', SYNC_RATE_LIMIT);

    expect(result.headers['X-RateLimit-Limit']).toBe('30');
  });

  it('sync should be more restrictive than api', () => {
    expect(SYNC_RATE_LIMIT.requests).toBeLessThan(API_RATE_LIMIT.requests);
  });

  it('auth should allow enough for device code polling', () => {
    // Device code polling is every 5 seconds = 12 requests/minute
    // Auth limit of 60 should be more than enough
    expect(AUTH_RATE_LIMIT.requests).toBeGreaterThan(12);
  });
});

describe('Rate Limit - Edge Cases', () => {
  let kv: MockRateLimitKV;

  beforeEach(() => {
    kv = new MockRateLimitKV();
  });

  it('should handle IPv6 addresses', async () => {
    const result = await checkRateLimit(
      kv,
      '2001:0db8:85a3:0000:0000:8a2e:0370:7334',
      { requests: 10, windowSeconds: 60 }
    );

    expect(result.allowed).toBe(true);
  });

  it('should handle unknown IP', async () => {
    const result = await checkRateLimit(
      kv,
      'unknown',
      { requests: 10, windowSeconds: 60 }
    );

    expect(result.allowed).toBe(true);
  });

  it('should handle concurrent requests from same IP', async () => {
    const config = { requests: 10, windowSeconds: 60 };
    const ip = '192.168.1.1';

    // Simulate 5 concurrent requests
    const results = await Promise.all([
      checkRateLimit(kv, ip, config),
      checkRateLimit(kv, ip, config),
      checkRateLimit(kv, ip, config),
      checkRateLimit(kv, ip, config),
      checkRateLimit(kv, ip, config),
    ]);

    // All should be allowed (though remaining counts may vary due to race)
    const allowedCount = results.filter(r => r.allowed).length;
    expect(allowedCount).toBe(5);
  });

  it('should handle IP with special characters', async () => {
    // This shouldn't happen in practice, but test defensive handling
    const result = await checkRateLimit(
      kv,
      '192.168.1.1, 10.0.0.1', // X-Forwarded-For with multiple IPs
      { requests: 10, windowSeconds: 60 }
    );

    expect(result.allowed).toBe(true);
  });
});

describe('Rate Limit - Counter Persistence', () => {
  let kv: MockRateLimitKV;

  beforeEach(() => {
    kv = new MockRateLimitKV();
  });

  it('should persist count across multiple checks', async () => {
    const config = { requests: 5, windowSeconds: 60 };
    const ip = '192.168.1.1';

    // Make 3 requests
    await checkRateLimit(kv, ip, config);
    await checkRateLimit(kv, ip, config);
    await checkRateLimit(kv, ip, config);

    // Check remaining is 2
    const result = await checkRateLimit(kv, ip, config);
    expect(result.headers['X-RateLimit-Remaining']).toBe('1');
  });

  it('should use correct key format', async () => {
    const config = { requests: 10, windowSeconds: 60 };

    await checkRateLimit(kv, '192.168.1.1', config);

    // Verify the key was stored correctly
    const stored = await kv.get('ratelimit:192.168.1.1');
    expect(stored).toBe('1');
  });

  it('should increment counter on each request', async () => {
    const config = { requests: 10, windowSeconds: 60 };
    const ip = '192.168.1.1';

    await checkRateLimit(kv, ip, config);
    expect(await kv.get(`ratelimit:${ip}`)).toBe('1');

    await checkRateLimit(kv, ip, config);
    expect(await kv.get(`ratelimit:${ip}`)).toBe('2');

    await checkRateLimit(kv, ip, config);
    expect(await kv.get(`ratelimit:${ip}`)).toBe('3');
  });
});

describe('Rate Limit - Response Format', () => {
  let kv: MockRateLimitKV;

  beforeEach(() => {
    kv = new MockRateLimitKV();
  });

  it('should return correct structure for allowed request', async () => {
    const result = await checkRateLimit(kv, '192.168.1.1', {
      requests: 10,
      windowSeconds: 60,
    });

    expect(result).toHaveProperty('allowed', true);
    expect(result).toHaveProperty('headers');
    expect(result).not.toHaveProperty('error');
    expect(result.status).toBeUndefined();
  });

  it('should return correct structure for blocked request', async () => {
    const config = { requests: 1, windowSeconds: 60 };
    const ip = '192.168.1.1';

    await checkRateLimit(kv, ip, config);
    const result = await checkRateLimit(kv, ip, config);

    expect(result).toHaveProperty('allowed', false);
    expect(result).toHaveProperty('status', 429);
    expect(result).toHaveProperty('error');
    expect(result).toHaveProperty('headers');
  });

  it('should include all required headers for 429 response', async () => {
    const config = { requests: 1, windowSeconds: 60 };
    const ip = '192.168.1.1';

    await checkRateLimit(kv, ip, config);
    const result = await checkRateLimit(kv, ip, config);

    expect(result.headers).toHaveProperty('X-RateLimit-Limit');
    expect(result.headers).toHaveProperty('X-RateLimit-Remaining');
    expect(result.headers).toHaveProperty('Retry-After');
  });
});
