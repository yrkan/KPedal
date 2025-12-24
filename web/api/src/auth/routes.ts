import { Hono } from 'hono';
import { Env, ApiResponse } from '../types/env';
import { createTokens, verifyRefreshToken, revokeRefreshToken } from './jwt';
import { getOrCreateUser } from './user';

const auth = new Hono<{ Bindings: Env }>();

// Device Code Flow constants
const DEVICE_CODE_EXPIRY = 600; // 10 minutes
const DEVICE_CODE_INTERVAL = 5; // Poll every 5 seconds
const MAX_CODE_REQUESTS_PER_DEVICE = 5; // Max 5 code requests per 10 min per device
const MAX_VERIFY_ATTEMPTS = 10; // Max 10 verify attempts per code
const MAX_POLL_RATE_MS = 4000; // Min 4 seconds between polls

/**
 * Rate limiter helper - returns true if rate limit exceeded
 */
async function checkRateLimit(
  kv: KVNamespace,
  key: string,
  maxAttempts: number,
  windowSeconds: number
): Promise<boolean> {
  const countStr = await kv.get(`ratelimit:${key}`);
  const count = countStr ? parseInt(countStr) : 0;

  if (count >= maxAttempts) {
    return true; // Rate limit exceeded
  }

  // Increment counter
  await kv.put(`ratelimit:${key}`, String(count + 1), {
    expirationTtl: windowSeconds
  });

  return false;
}

/**
 * Generate a user-friendly code like "ABCD-1234"
 * Uses crypto.getRandomValues for cryptographically secure randomness
 */
function generateUserCode(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ'; // No I, O to avoid confusion
  const nums = '0123456789';

  // Get cryptographically secure random bytes
  const randomBytes = new Uint8Array(8);
  crypto.getRandomValues(randomBytes);

  let code = '';
  for (let i = 0; i < 4; i++) {
    code += chars.charAt(randomBytes[i] % chars.length);
  }
  code += '-';
  for (let i = 4; i < 8; i++) {
    code += nums.charAt(randomBytes[i] % nums.length);
  }
  return code;
}

/**
 * POST /auth/device/code
 * Start device authorization flow - generates codes for device
 */
auth.post('/device/code', async (c) => {
  const env = c.env;

  try {
    const body = await c.req.json<{
      device_id: string;
      device_name?: string;
    }>();

    const { device_id, device_name = 'Karoo' } = body;

    if (!device_id) {
      return c.json<ApiResponse>({
        success: false,
        error: 'Missing device_id',
      }, 400);
    }

    // Validate device_id format (should be UUID-like)
    if (device_id.length < 8 || device_id.length > 64) {
      return c.json<ApiResponse>({
        success: false,
        error: 'Invalid device_id format',
      }, 400);
    }

    // Rate limit: max 5 requests per device per 10 minutes
    const rateLimited = await checkRateLimit(
      env.SESSIONS,
      `device_code:${device_id}`,
      MAX_CODE_REQUESTS_PER_DEVICE,
      DEVICE_CODE_EXPIRY
    );

    if (rateLimited) {
      return c.json<ApiResponse>({
        success: false,
        error: 'Too many requests. Please try again later.',
      }, 429);
    }

    // Generate codes
    const deviceCode = crypto.randomUUID();
    const userCode = generateUserCode();

    // Store device code data in KV
    const deviceData = {
      deviceId: device_id,
      deviceName: device_name,
      userCode,
      status: 'pending', // pending | authorized | expired
      createdAt: Date.now(),
    };

    await env.SESSIONS.put(
      `device_code:${deviceCode}`,
      JSON.stringify(deviceData),
      { expirationTtl: DEVICE_CODE_EXPIRY }
    );

    // Also store by user code for lookup during authorization
    await env.SESSIONS.put(
      `user_code:${userCode}`,
      deviceCode,
      { expirationTtl: DEVICE_CODE_EXPIRY }
    );

    return c.json<ApiResponse>({
      success: true,
      data: {
        device_code: deviceCode,
        user_code: userCode,
        verification_uri: `${env.APP_URL}/link`,
        expires_in: DEVICE_CODE_EXPIRY,
        interval: DEVICE_CODE_INTERVAL,
      },
    });

  } catch (err) {
    console.error('Device code error:', err);
    return c.json<ApiResponse>({
      success: false,
      error: 'Failed to generate device code',
    }, 500);
  }
});

/**
 * POST /auth/device/token
 * Device polls this to check if user has authorized
 */
auth.post('/device/token', async (c) => {
  const env = c.env;

  try {
    const body = await c.req.json<{
      device_code: string;
      device_id?: string;
    }>();

    const { device_code, device_id } = body;

    if (!device_code) {
      return c.json<ApiResponse>({
        success: false,
        error: 'Missing device_code',
      }, 400);
    }

    // Validate device_code format (should be UUID)
    if (device_code.length !== 36) {
      return c.json<ApiResponse>({
        success: false,
        error: 'Invalid device_code format',
      }, 400);
    }

    // Check poll rate limiting (min 4 seconds between polls)
    const lastPollKey = `poll_time:${device_code}`;
    const lastPollStr = await env.SESSIONS.get(lastPollKey);
    const now = Date.now();

    if (lastPollStr) {
      const lastPoll = parseInt(lastPollStr);
      if (now - lastPoll < MAX_POLL_RATE_MS) {
        return c.json<ApiResponse>({
          success: false,
          status: 'slow_down',
          error: 'slow_down',
          error_description: 'You are polling too quickly',
        }, 400);
      }
    }

    // Update last poll time
    await env.SESSIONS.put(lastPollKey, String(now), { expirationTtl: 60 });

    // Get device data from KV
    const deviceDataStr = await env.SESSIONS.get(`device_code:${device_code}`);

    if (!deviceDataStr) {
      return c.json<ApiResponse>({
        success: false,
        status: 'expired',
        error: 'expired_token',
        error_description: 'The device code has expired',
      }, 400);
    }

    const deviceData = JSON.parse(deviceDataStr) as {
      deviceId: string;
      deviceName: string;
      userCode: string;
      status: string;
      userId?: string;
      createdAt: number;
    };

    if (deviceData.status === 'pending') {
      // User hasn't authorized yet
      return c.json<ApiResponse>({
        success: false,
        status: 'authorization_pending',
        error: 'authorization_pending',
        error_description: 'The user has not yet authorized this device',
      }, 400);
    }

    if (deviceData.status === 'authorized' && deviceData.userId) {
      // User has authorized! Get user and create tokens
      const result = await env.DB.prepare(
        'SELECT id, email, name, picture FROM users WHERE id = ?'
      ).bind(deviceData.userId).first<{
        id: string;
        email: string;
        name: string;
        picture: string | null;
      }>();

      if (!result) {
        return c.json<ApiResponse>({
          success: false,
          error: 'User not found',
        }, 500);
      }

      // Create tokens
      const { accessToken, refreshToken } = await createTokens(env, result);

      // Store refresh token
      await env.SESSIONS.put(
        `refresh:${result.id}:${refreshToken.slice(-16)}`,
        JSON.stringify({
          createdAt: Date.now(),
          deviceId: deviceData.deviceId,
          deviceName: deviceData.deviceName,
        }),
        { expirationTtl: 60 * 60 * 24 * 7 } // 7 days
      );

      // Clean up device codes
      await env.SESSIONS.delete(`device_code:${device_code}`);
      await env.SESSIONS.delete(`user_code:${deviceData.userCode}`);

      return c.json<ApiResponse>({
        success: true,
        data: {
          access_token: accessToken,
          refresh_token: refreshToken,
          user: {
            id: result.id,
            email: result.email,
            name: result.name,
            picture: result.picture,
          },
        },
      });
    }

    return c.json<ApiResponse>({
      success: false,
      error: 'Unknown status',
    }, 500);

  } catch (err) {
    console.error('Device token error:', err);
    return c.json<ApiResponse>({
      success: false,
      error: 'Failed to get token',
    }, 500);
  }
});

/**
 * POST /auth/device/authorize
 * Called by web app when user enters code and logs in
 */
auth.post('/device/authorize', async (c) => {
  const env = c.env;

  try {
    const body = await c.req.json<{
      user_code: string;
      user_id: string;
    }>();

    const { user_code, user_id } = body;

    if (!user_code || !user_id) {
      return c.json<ApiResponse>({
        success: false,
        error: 'Missing user_code or user_id',
      }, 400);
    }

    // Validate user_id format
    if (user_id.length < 8 || user_id.length > 64) {
      return c.json<ApiResponse>({
        success: false,
        error: 'Invalid user_id format',
      }, 400);
    }

    // Normalize user code (uppercase, remove spaces, ensure dash format XXXX-XXXX)
    let normalizedCode = user_code.toUpperCase().replace(/\s/g, '').replace(/-/g, '');

    // Validate code format (8 alphanumeric chars: 4 letters + 4 digits)
    if (!/^[A-Z]{4}[0-9]{4}$/.test(normalizedCode)) {
      return c.json<ApiResponse>({
        success: false,
        error: 'Invalid code format',
      }, 400);
    }

    // Add dash for lookup
    normalizedCode = normalizedCode.slice(0, 4) + '-' + normalizedCode.slice(4);

    // Get device code from user code
    const deviceCode = await env.SESSIONS.get(`user_code:${normalizedCode}`);

    if (!deviceCode) {
      return c.json<ApiResponse>({
        success: false,
        error: 'Invalid or expired code',
      }, 400);
    }

    // Get device data
    const deviceDataStr = await env.SESSIONS.get(`device_code:${deviceCode}`);

    if (!deviceDataStr) {
      return c.json<ApiResponse>({
        success: false,
        error: 'Device code expired',
      }, 400);
    }

    const deviceData = JSON.parse(deviceDataStr);

    // Mark as authorized
    deviceData.status = 'authorized';
    deviceData.userId = user_id;

    // Update device data
    await env.SESSIONS.put(
      `device_code:${deviceCode}`,
      JSON.stringify(deviceData),
      { expirationTtl: 60 } // Keep for 1 minute for device to poll
    );

    return c.json<ApiResponse>({
      success: true,
      data: {
        device_name: deviceData.deviceName,
      },
    });

  } catch (err) {
    console.error('Device authorize error:', err);
    return c.json<ApiResponse>({
      success: false,
      error: 'Failed to authorize device',
    }, 500);
  }
});

/**
 * GET /auth/device/verify
 * Check if a user code is valid (used by web app before showing login)
 */
auth.get('/device/verify', async (c) => {
  const env = c.env;

  const userCode = c.req.query('code');

  if (!userCode) {
    return c.json<ApiResponse>({
      success: false,
      error: 'Missing code parameter',
    }, 400);
  }

  // Normalize user code (uppercase, remove spaces and dashes)
  let normalizedCode = userCode.toUpperCase().replace(/\s/g, '').replace(/-/g, '');

  // Validate code format (8 chars: 4 letters + 4 digits)
  if (!/^[A-Z]{4}[0-9]{4}$/.test(normalizedCode)) {
    return c.json<ApiResponse>({
      success: false,
      error: 'Invalid code format',
    }, 400);
  }

  // Add dash for lookup
  normalizedCode = normalizedCode.slice(0, 4) + '-' + normalizedCode.slice(4);

  // Rate limit verification attempts per IP
  const clientIP = c.req.header('CF-Connecting-IP') || c.req.header('X-Forwarded-For') || 'unknown';
  const rateLimited = await checkRateLimit(
    env.SESSIONS,
    `verify_ip:${clientIP}`,
    MAX_VERIFY_ATTEMPTS,
    300 // 5 minutes
  );

  if (rateLimited) {
    return c.json<ApiResponse>({
      success: false,
      error: 'Too many attempts. Please try again later.',
    }, 429);
  }

  // Check if code exists
  const deviceCode = await env.SESSIONS.get(`user_code:${normalizedCode}`);

  if (!deviceCode) {
    return c.json<ApiResponse>({
      success: false,
      error: 'Invalid or expired code',
    }, 400);
  }

  // Get device data for display
  const deviceDataStr = await env.SESSIONS.get(`device_code:${deviceCode}`);
  if (!deviceDataStr) {
    return c.json<ApiResponse>({
      success: false,
      error: 'Device code expired',
    }, 400);
  }

  const deviceData = JSON.parse(deviceDataStr);

  return c.json<ApiResponse>({
    success: true,
    data: {
      device_name: deviceData.deviceName,
      valid: true,
    },
  });
});

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
 * POST /auth/mobile/login
 * Login from mobile app using Google ID token
 */
auth.post('/mobile/login', async (c) => {
  const env = c.env;

  try {
    const body = await c.req.json<{
      id_token: string;
      device_id: string;
      device_name?: string;
    }>();

    const { id_token, device_id, device_name = 'Karoo' } = body;

    if (!id_token || !device_id) {
      return c.json<ApiResponse>({
        success: false,
        error: 'Missing id_token or device_id',
      }, 400);
    }

    // Verify Google ID token
    const tokenInfoResponse = await fetch(
      `https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(id_token)}`
    );

    if (!tokenInfoResponse.ok) {
      return c.json<ApiResponse>({
        success: false,
        error: 'Invalid Google ID token',
      }, 401);
    }

    const tokenInfo = await tokenInfoResponse.json() as {
      aud: string;
      sub: string;
      email: string;
      email_verified: string;
      name: string;
      picture: string;
    };

    // Verify the token was issued for our app
    if (tokenInfo.aud !== env.GOOGLE_CLIENT_ID) {
      return c.json<ApiResponse>({
        success: false,
        error: 'Token not issued for this app',
      }, 401);
    }

    // Get or create user in our database
    const user = await getOrCreateUser(env.DB, {
      googleId: tokenInfo.sub,
      email: tokenInfo.email,
      name: tokenInfo.name,
      picture: tokenInfo.picture,
    });

    // Create our JWT tokens
    const { accessToken, refreshToken } = await createTokens(env, user);

    // Store refresh token in KV (for revocation capability)
    await env.SESSIONS.put(
      `refresh:${user.id}:${refreshToken.slice(-16)}`,
      JSON.stringify({
        createdAt: Date.now(),
        deviceId: device_id,
        deviceName: device_name,
      }),
      { expirationTtl: 60 * 60 * 24 * 7 } // 7 days
    );

    // Return tokens and user info
    return c.json<ApiResponse>({
      success: true,
      data: {
        access_token: accessToken,
        refresh_token: refreshToken,
        user: {
          id: user.id,
          email: user.email,
          name: user.name,
          picture: user.picture,
        },
      },
    });

  } catch (err) {
    console.error('Mobile login error:', err);
    return c.json<ApiResponse>({
      success: false,
      error: 'Login failed',
    }, 500);
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
