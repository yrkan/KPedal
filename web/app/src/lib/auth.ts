import { writable, derived, get } from 'svelte/store';
import { browser } from '$app/environment';
import { goto } from '$app/navigation';
import { API_URL, TOKEN_REFRESH_BUFFER_MS } from './config';

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

  function login(accessToken: string, refreshToken: string) {
    const payload = parseJWT(accessToken);
    if (!payload) return;

    secureStore({ accessToken, refreshToken });
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
    });
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

    set({
      user: null,
      accessToken: null,
      refreshToken: null,
      loading: false,
    });

    goto('/login');
  }

  async function initialize() {
    if (!browser) return;

    const { accessToken, refreshToken } = secureRetrieve();

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
        });
        return;
      }
    }

    // Try to refresh
    const refreshed = await refresh();
    if (!refreshed) {
      set({ ...initialState, loading: false });
    }
  }

  return {
    subscribe,
    login,
    logout,
    refresh,
    initialize,
  };
}

export const auth = createAuthStore();

// Derived stores for convenience
export const user = derived(auth, ($auth) => $auth.user);
export const isAuthenticated = derived(auth, ($auth) => !!$auth.user);
export const isLoading = derived(auth, ($auth) => $auth.loading);

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

  return res;
}
