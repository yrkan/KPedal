import { Hono } from 'hono';
import { Env, ApiResponse } from '../types/env';
import { createTokens, verifyRefreshToken, revokeRefreshToken } from './jwt';
import { getOrCreateUser } from './user';

const auth = new Hono<{ Bindings: Env }>();

/**
 * GET /auth/login
 * Redirects to Google OAuth consent screen
 */
auth.get('/login', async (c) => {
  const env = c.env;

  // Generate state for CSRF protection
  const state = crypto.randomUUID();

  // Store state in KV with 10 min expiry
  await env.SESSIONS.put(`oauth_state:${state}`, '1', { expirationTtl: 600 });

  const params = new URLSearchParams({
    client_id: env.GOOGLE_CLIENT_ID,
    redirect_uri: `${env.API_URL}/auth/callback`,
    response_type: 'code',
    scope: 'openid email profile',
    state,
    access_type: 'offline',
    prompt: 'consent',
  });

  return c.redirect(`https://accounts.google.com/o/oauth2/v2/auth?${params}`);
});

/**
 * GET /auth/callback
 * Handles Google OAuth callback
 */
auth.get('/callback', async (c) => {
  const env = c.env;
  const code = c.req.query('code');
  const state = c.req.query('state');
  const error = c.req.query('error');

  // Handle OAuth errors
  if (error) {
    return c.redirect(`${env.APP_URL}/login?error=${error}`);
  }

  if (!code || !state) {
    return c.redirect(`${env.APP_URL}/login?error=missing_params`);
  }

  // Verify state (CSRF protection)
  const storedState = await env.SESSIONS.get(`oauth_state:${state}`);
  if (!storedState) {
    return c.redirect(`${env.APP_URL}/login?error=invalid_state`);
  }
  await env.SESSIONS.delete(`oauth_state:${state}`);

  try {
    // Exchange code for tokens
    const tokenResponse = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        client_id: env.GOOGLE_CLIENT_ID,
        client_secret: env.GOOGLE_CLIENT_SECRET,
        code,
        grant_type: 'authorization_code',
        redirect_uri: `${env.API_URL}/auth/callback`,
      }),
    });

    if (!tokenResponse.ok) {
      console.error('Token exchange failed:', await tokenResponse.text());
      return c.redirect(`${env.APP_URL}/login?error=token_exchange_failed`);
    }

    const tokens = await tokenResponse.json() as {
      access_token: string;
      id_token: string;
    };

    // Get user info from Google
    const userInfoResponse = await fetch('https://www.googleapis.com/oauth2/v2/userinfo', {
      headers: { Authorization: `Bearer ${tokens.access_token}` },
    });

    if (!userInfoResponse.ok) {
      return c.redirect(`${env.APP_URL}/login?error=userinfo_failed`);
    }

    const googleUser = await userInfoResponse.json() as {
      id: string;
      email: string;
      name: string;
      picture: string;
    };

    // Get or create user in our database
    const user = await getOrCreateUser(env.DB, {
      googleId: googleUser.id,
      email: googleUser.email,
      name: googleUser.name,
      picture: googleUser.picture,
    });

    // Create our JWT tokens
    const { accessToken, refreshToken } = await createTokens(env, user);

    // Store refresh token in KV (for revocation capability)
    await env.SESSIONS.put(
      `refresh:${user.id}:${refreshToken.slice(-16)}`,
      JSON.stringify({ createdAt: Date.now() }),
      { expirationTtl: 60 * 60 * 24 * 7 } // 7 days
    );

    // Redirect to frontend with tokens
    const params = new URLSearchParams({
      access_token: accessToken,
      refresh_token: refreshToken,
    });

    return c.redirect(`${env.APP_URL}/auth/success?${params}`);

  } catch (err) {
    console.error('Auth callback error:', err);
    return c.redirect(`${env.APP_URL}/login?error=auth_failed`);
  }
});

/**
 * POST /auth/refresh
 * Refresh access token using refresh token
 */
auth.post('/refresh', async (c) => {
  const env = c.env;

  try {
    const body = await c.req.json<{ refresh_token: string }>();
    const { refresh_token } = body;

    if (!refresh_token) {
      return c.json<ApiResponse>({ success: false, error: 'Missing refresh token' }, 400);
    }

    // Verify refresh token
    const payload = await verifyRefreshToken(env, refresh_token);
    if (!payload) {
      return c.json<ApiResponse>({ success: false, error: 'Invalid refresh token' }, 401);
    }

    // Check if token is still valid in KV (not revoked)
    const tokenKey = `refresh:${payload.sub}:${refresh_token.slice(-16)}`;
    const storedToken = await env.SESSIONS.get(tokenKey);
    if (!storedToken) {
      return c.json<ApiResponse>({ success: false, error: 'Token revoked' }, 401);
    }

    // Create new access token
    const { accessToken } = await createTokens(env, {
      id: payload.sub,
      email: payload.email,
      name: payload.name,
      picture: payload.picture,
    });

    return c.json<ApiResponse>({
      success: true,
      data: { access_token: accessToken },
    });

  } catch (err) {
    console.error('Refresh error:', err);
    return c.json<ApiResponse>({ success: false, error: 'Refresh failed' }, 500);
  }
});

/**
 * POST /auth/logout
 * Revoke refresh token
 */
auth.post('/logout', async (c) => {
  const env = c.env;

  try {
    const body = await c.req.json<{ refresh_token: string }>();
    const { refresh_token } = body;

    if (refresh_token) {
      const payload = await verifyRefreshToken(env, refresh_token);
      if (payload) {
        await revokeRefreshToken(env, payload.sub, refresh_token);
      }
    }

    return c.json<ApiResponse>({ success: true, message: 'Logged out' });

  } catch (err) {
    // Still return success even if token was invalid
    return c.json<ApiResponse>({ success: true, message: 'Logged out' });
  }
});

export { auth as authRoutes };
