/**
 * Demo data caching utility
 *
 * Uses Cloudflare Cache API for fastest edge-local caching.
 * Cache key includes date since timestamps are adjusted daily.
 *
 * Cache API is ~5ms vs KV ~50ms for reads.
 */

import { isDemoUser } from '../types/env';

// Cache TTL: 24 hours (demo offset changes daily)
const CACHE_TTL_SECONDS = 24 * 60 * 60;

// Base URL for cache keys (must be valid URL format)
const CACHE_URL_BASE = 'https://cache.kpedal.com/demo';

/**
 * Get cache key URL for demo data
 */
function getCacheUrl(endpoint: string): string {
  const today = new Date().toISOString().split('T')[0];
  // URL encode the endpoint to handle slashes
  const encodedEndpoint = encodeURIComponent(endpoint);
  return `${CACHE_URL_BASE}/${encodedEndpoint}/${today}`;
}

/**
 * Get cached demo response from Cache API
 */
export async function getDemoCache<T>(
  _kv: KVNamespace, // Keep parameter for backwards compatibility, unused
  userId: string,
  endpoint: string
): Promise<T | null> {
  if (!isDemoUser(userId)) return null;

  try {
    const cache = caches.default;
    const cacheUrl = getCacheUrl(endpoint);
    const cacheKey = new Request(cacheUrl);

    const cached = await cache.match(cacheKey);
    if (cached) {
      const data = await cached.json();
      return data as T;
    }
  } catch {
    // Cache API error, return null
  }

  return null;
}

/**
 * Set demo response in Cache API
 */
export async function setDemoCache<T>(
  _kv: KVNamespace, // Keep parameter for backwards compatibility, unused
  userId: string,
  endpoint: string,
  data: T
): Promise<void> {
  if (!isDemoUser(userId)) return;

  try {
    const cache = caches.default;
    const cacheUrl = getCacheUrl(endpoint);
    const cacheKey = new Request(cacheUrl);

    const response = new Response(JSON.stringify(data), {
      headers: {
        'Content-Type': 'application/json',
        'Cache-Control': `public, max-age=${CACHE_TTL_SECONDS}`,
      },
    });

    await cache.put(cacheKey, response);
  } catch {
    // Cache API error, ignore
  }
}

/**
 * Wrapper for cached demo endpoint
 * Returns cached data if available, otherwise executes fetcher and caches result
 */
export async function withDemoCache<T>(
  kv: KVNamespace,
  userId: string,
  endpoint: string,
  fetcher: () => Promise<T>
): Promise<{ data: T; cached: boolean }> {
  // Check cache for demo users
  if (isDemoUser(userId)) {
    const cached = await getDemoCache<T>(kv, userId, endpoint);
    if (cached) {
      return { data: cached, cached: true };
    }
  }

  // Fetch fresh data
  const data = await fetcher();

  // Cache for demo users
  if (isDemoUser(userId)) {
    // Don't await - cache in background
    setDemoCache(kv, userId, endpoint, data).catch(() => {});
  }

  return { data, cached: false };
}
