import { writable, derived, get } from 'svelte/store';
import { browser } from '$app/environment';
import { goto } from '$app/navigation';
import { API_URL, TOKEN_REFRESH_BUFFER_MS } from './config';

// ============================================
// Demo Browser Cache
// ============================================

const DEMO_CACHE_PREFIX = 'kpedal_demo_cache:';
const DEMO_CACHE_TTL_MS = 60 * 60 * 1000; // 1 hour

interface CachedData<T> {
  data: T;
  timestamp: number;
}

function getDemoCacheKey(path: string): string {
  // Include date in key since demo timestamps are adjusted daily
  const today = new Date().toISOString().split('T')[0];
  return `${DEMO_CACHE_PREFIX}${path}:${today}`;
}

function getDemoCache<T>(path: string): T | null {
  if (!browser) return null;
  try {
    const key = getDemoCacheKey(path);
    const cached = sessionStorage.getItem(key);
    if (!cached) return null;

    const { data, timestamp }: CachedData<T> = JSON.parse(cached);
    // Check if cache is still fresh
    if (Date.now() - timestamp > DEMO_CACHE_TTL_MS) {
      sessionStorage.removeItem(key);
      return null;
    }
    return data;
  } catch {
    return null;
  }
}

function setDemoCache<T>(path: string, data: T): void {
  if (!browser) return;
  try {
    const key = getDemoCacheKey(path);
    const cached: CachedData<T> = { data, timestamp: Date.now() };
    sessionStorage.setItem(key, JSON.stringify(cached));
  } catch {
    // Ignore storage errors (quota exceeded, etc.)
  }
}

function clearDemoCache(): void {
  if (!browser) return;
  // Clear all demo cache keys
  const keysToRemove: string[] = [];
  for (let i = 0; i < sessionStorage.length; i++) {
    const key = sessionStorage.key(i);
    if (key?.startsWith(DEMO_CACHE_PREFIX)) {
      keysToRemove.push(key);
    }
  }
  keysToRemove.forEach(key => sessionStorage.removeItem(key));
}

// ============================================
// Types
// ============================================

interface User {
  id: string;
  email: string;
  name: string;
  picture?: string;
}

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  loading: boolean;
  isDemo: boolean;
}

interface JWTPayload {
  sub: string;
  email: string;
  name: string;
  picture?: string;
  exp: number;
  iat: number;
}

// ============================================
// Secure Token Storage
// ============================================

const STORAGE_KEY = 'kpedal_auth';

function secureStore(data: { accessToken: string; refreshToken: string } | null): void {
  if (!browser) return;

  if (data) {
    // Store in sessionStorage (cleared on tab close) for better security
    // Use localStorage only for refresh token persistence
    sessionStorage.setItem(STORAGE_KEY + '_access', data.accessToken);
    localStorage.setItem(STORAGE_KEY + '_refresh', data.refreshToken);
  } else {
    sessionStorage.removeItem(STORAGE_KEY + '_access');
    localStorage.removeItem(STORAGE_KEY + '_refresh');
  }
}

function secureRetrieve(): { accessToken: string | null; refreshToken: string | null } {
  if (!browser) return { accessToken: null, refreshToken: null };

  return {
    accessToken: sessionStorage.getItem(STORAGE_KEY + '_access'),
    refreshToken: localStorage.getItem(STORAGE_KEY + '_refresh'),
  };
}

// ============================================
// JWT Parsing (client-side only, no verification)
// ============================================

function parseJWT(token: string): JWTPayload | null {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

function isTokenExpired(token: string, bufferMs: number = 0): boolean {
  const payload = parseJWT(token);
  if (!payload) return true;
  return Date.now() >= (payload.exp * 1000) - bufferMs;
}

// ============================================
// Auth Store
// ============================================

function createAuthStore() {
  const initialState: AuthState = {
    user: null,
    accessToken: null,
    refreshToken: null,
    loading: true,
    isDemo: false,
  };

  const { subscribe, set, update } = writable<AuthState>(initialState);

  let refreshTimeout: ReturnType<typeof setTimeout> | null = null;

  function scheduleRefresh(accessToken: string) {
    if (refreshTimeout) clearTimeout(refreshTimeout);

    const payload = parseJWT(accessToken);
    if (!payload) return;

    const expiresIn = (payload.exp * 1000) - Date.now() - TOKEN_REFRESH_BUFFER_MS;

    if (expiresIn > 0) {
      refreshTimeout = setTimeout(() => {
        refresh();
      }, expiresIn);
    }
  }

  async function refresh(): Promise<boolean> {
    const { refreshToken } = secureRetrieve();
    if (!refreshToken || isTokenExpired(refreshToken)) {
      logout();
      return false;
    }

    try {
      const res = await fetch(`${API_URL}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refresh_token: refreshToken }),
      });

      if (!res.ok) {
        logout();
        return false;
      }

      const data = await res.json();
      if (data.success && data.data?.access_token) {
        const accessToken = data.data.access_token;
        const payload = parseJWT(accessToken);

        if (payload) {
          secureStore({ accessToken, refreshToken });
          scheduleRefresh(accessToken);

          update((state) => ({
            ...state,
            accessToken,
            user: {
              id: payload.sub,
              email: payload.email,
              name: payload.name,
              picture: payload.picture,
            },
          }));
          return true;
        }
      }
    } catch (err) {
      console.error('Token refresh failed:', err);
    }

    logout();
    return false;
  }

  function login(accessToken: string, refreshToken: string, isDemo: boolean = false) {
    const payload = parseJWT(accessToken);
    if (!payload) return;

    secureStore({ accessToken, refreshToken });
    if (isDemo) {
      localStorage.setItem(STORAGE_KEY + '_demo', 'true');
    } else {
      localStorage.removeItem(STORAGE_KEY + '_demo');
    }
    scheduleRefresh(accessToken);

    set({
      user: {
        id: payload.sub,
        email: payload.email,
        name: payload.name,
        picture: payload.picture,
      },
      accessToken,
      refreshToken,
      loading: false,
      isDemo,
    });
  }

  async function demoLogin(): Promise<boolean> {
    try {
      const res = await fetch(`${API_URL}/auth/demo`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      });

      if (!res.ok) {
        return false;
      }

      const data = await res.json();
      if (data.success && data.data?.access_token && data.data?.refresh_token) {
        login(data.data.access_token, data.data.refresh_token, true);
        // No prefetch needed - demo uses static data from demoData.ts
        return true;
      }

      return false;
    } catch (err) {
      console.error('Demo login failed:', err);
      return false;
    }
  }

  async function logout() {
    const { refreshToken } = secureRetrieve();

    // Revoke refresh token on server
    if (refreshToken) {
      try {
        await fetch(`${API_URL}/auth/logout`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refresh_token: refreshToken }),
        });
      } catch {
        // Ignore errors - still logout locally
      }
    }

    if (refreshTimeout) clearTimeout(refreshTimeout);
    secureStore(null);
    localStorage.removeItem(STORAGE_KEY + '_demo');
    clearDemoCache(); // Clear browser cache for demo data

    set({
      user: null,
      accessToken: null,
      refreshToken: null,
      loading: false,
      isDemo: false,
    });

    goto('/login');
  }

  async function initialize() {
    if (!browser) return;

    const { accessToken, refreshToken } = secureRetrieve();
    const isDemo = localStorage.getItem(STORAGE_KEY + '_demo') === 'true';

    if (!refreshToken) {
      set({ ...initialState, loading: false });
      return;
    }

    // If access token exists and valid, use it
    if (accessToken && !isTokenExpired(accessToken, TOKEN_REFRESH_BUFFER_MS)) {
      const payload = parseJWT(accessToken);
      if (payload) {
        scheduleRefresh(accessToken);
        set({
          user: {
            id: payload.sub,
            email: payload.email,
            name: payload.name,
            picture: payload.picture,
          },
          accessToken,
          refreshToken,
          loading: false,
          isDemo,
        });
        return;
      }
    }

    // Try to refresh
    const refreshed = await refresh();
    if (!refreshed) {
      set({ ...initialState, loading: false });
    } else {
      // Restore demo flag after refresh
      update(state => ({ ...state, isDemo }));
    }
  }

  return {
    subscribe,
    login,
    logout,
    refresh,
    initialize,
    demoLogin,
  };
}

export const auth = createAuthStore();

// Derived stores for convenience
export const user = derived(auth, ($auth) => $auth.user);
export const isAuthenticated = derived(auth, ($auth) => !!$auth.user);
export const isLoading = derived(auth, ($auth) => $auth.loading);
export const isDemo = derived(auth, ($auth) => $auth.isDemo);

// ============================================
// Authenticated Fetch Helper
// ============================================

export async function authFetch(
  path: string,
  options: RequestInit = {}
): Promise<Response> {
  const state = get(auth);

  if (!state.accessToken) {
    throw new Error('Not authenticated');
  }

  // For demo users: check browser cache first (GET requests only)
  const method = options.method?.toUpperCase() || 'GET';
  if (state.isDemo && method === 'GET') {
    const cached = getDemoCache<unknown>(path);
    if (cached) {
      // Return a fake Response with cached data
      return new Response(JSON.stringify({ success: true, data: cached }), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
          'X-Cache': 'BROWSER',
        },
      });
    }
  }

  // Check if token needs refresh
  if (isTokenExpired(state.accessToken, TOKEN_REFRESH_BUFFER_MS)) {
    const refreshed = await auth.refresh();
    if (!refreshed) {
      throw new Error('Session expired');
    }
  }

  const currentState = get(auth);

  const res = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${currentState.accessToken}`,
      ...options.headers,
    },
  });

  // If unauthorized, try refresh once
  if (res.status === 401) {
    const refreshed = await auth.refresh();
    if (refreshed) {
      const newState = get(auth);
      return fetch(`${API_URL}${path}`, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${newState.accessToken}`,
          ...options.headers,
        },
      });
    }
  }

  // For demo users: cache successful GET responses
  if (currentState.isDemo && method === 'GET' && res.ok) {
    // Clone response to read body without consuming it
    const clone = res.clone();
    clone.json().then((json) => {
      if (json.success && json.data) {
        setDemoCache(path, json.data);
      }
    }).catch(() => {});
  }

  return res;
}
