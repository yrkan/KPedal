import { Hono } from 'hono';
import { Env, ApiResponse } from '../types/env';
import { createTokens, verifyRefreshToken, revokeRefreshToken } from './jwt';
import { getOrCreateUser } from './user';

const auth = new Hono<{ Bindings: Env }>();

// Device Code Flow constants
const DEVICE_CODE_EXPIRY_SECONDS = 600; // 10 minutes
const DEVICE_CODE_INTERVAL = 5; // Poll every 5 seconds (fast response)
const MAX_VERIFY_ATTEMPTS = 20; // Max 20 verify attempts per IP per 5 min
const MIN_POLL_INTERVAL_MS = 4000; // Min 4 seconds between polls

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
 * Calculate expiry timestamp (10 minutes from now)
 */
function getExpiryTimestamp(): string {
  const expiry = new Date(Date.now() + DEVICE_CODE_EXPIRY_SECONDS * 1000);
  return expiry.toISOString().replace('T', ' ').replace('Z', '');
}

/**
 * Check if a device code is expired
 */
function isExpired(expiresAt: string): boolean {
  const expiry = new Date(expiresAt.replace(' ', 'T') + 'Z');
  return Date.now() > expiry.getTime();
}

/**
 * Get remaining seconds until expiry
 */
function getRemainingSeconds(expiresAt: string): number {
  const expiry = new Date(expiresAt.replace(' ', 'T') + 'Z');
  return Math.max(0, Math.floor((expiry.getTime() - Date.now()) / 1000));
}

/**
 * POST /auth/device/code
 * Start device authorization flow - generates codes for device
 * Uses D1 database for reliable storage (no eventual consistency issues)
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

    // Clean up expired codes for this device first
    await env.DB.prepare(
      `DELETE FROM device_codes WHERE device_id = ? AND expires_at < datetime('now')`
    ).bind(device_id).run();

    // Check if device already has an active code (pending or authorized)
    const existing = await env.DB.prepare(
      `SELECT device_code, user_code, status, expires_at
       FROM device_codes
       WHERE device_id = ? AND expires_at > datetime('now')
       ORDER BY created_at DESC LIMIT 1`
    ).bind(device_id).first<{
      device_code: string;
      user_code: string;
      status: string;
      expires_at: string;
    }>();

    if (existing && (existing.status === 'pending' || existing.status === 'authorized')) {
      // Return existing code
      const remainingTime = getRemainingSeconds(existing.expires_at);
      return c.json<ApiResponse>({
        success: true,
        data: {
          device_code: existing.device_code,
          user_code: existing.user_code,
          verification_uri: env.LINK_URL,
          expires_in: remainingTime,
          interval: DEVICE_CODE_INTERVAL,
        },
      });
    }

    // Generate new codes
    const deviceCode = crypto.randomUUID();
    const userCode = generateUserCode();
    const expiresAt = getExpiryTimestamp();

    // Insert into D1 database
    await env.DB.prepare(
      `INSERT INTO device_codes (device_code, user_code, device_id, device_name, status, expires_at)
       VALUES (?, ?, ?, ?, 'pending', ?)`
    ).bind(deviceCode, userCode, device_id, device_name, expiresAt).run();

    return c.json<ApiResponse>({
      success: true,
      data: {
        device_code: deviceCode,
        user_code: userCode,
        verification_uri: env.LINK_URL,
        expires_in: DEVICE_CODE_EXPIRY_SECONDS,
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
 * Uses D1 database for immediate consistency
 */
auth.post('/device/token', async (c) => {
  const env = c.env;

  try {
    const body = await c.req.json<{
      device_code: string;
      device_id?: string;
    }>();

    const { device_code } = body;

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
      if (now - lastPoll < MIN_POLL_INTERVAL_MS) {
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

    // Get device code from D1 database (immediate consistency!)
    const deviceData = await env.DB.prepare(
      `SELECT device_code, user_code, device_id, device_name, status, user_id, expires_at
       FROM device_codes WHERE device_code = ?`
    ).bind(device_code).first<{
      device_code: string;
      user_code: string;
      device_id: string;
      device_name: string;
      status: string;
      user_id: string | null;
      expires_at: string;
    }>();

    console.log('Token poll:', {
      deviceCode: device_code.slice(0, 8) + '...',
      found: !!deviceData,
      status: deviceData?.status,
      hasUserId: !!deviceData?.user_id,
      userCode: deviceData?.user_code,
    });

    if (!deviceData) {
      return c.json<ApiResponse>({
        success: false,
        status: 'expired',
        error: 'expired_token',
        error_description: 'The device code has expired',
      }, 400);
    }

    // Check if expired
    if (isExpired(deviceData.expires_at)) {
      // Clean up expired code
      await env.DB.prepare('DELETE FROM device_codes WHERE device_code = ?')
        .bind(device_code).run();

      return c.json<ApiResponse>({
        success: false,
        status: 'expired',
        error: 'expired_token',
        error_description: 'The device code has expired',
      }, 400);
    }

    if (deviceData.status === 'pending') {
      // User hasn't authorized yet
      return c.json<ApiResponse>({
        success: false,
        status: 'authorization_pending',
        error: 'authorization_pending',
        error_description: 'The user has not yet authorized this device',
      }, 400);
    }

    if (deviceData.status === 'authorized' && deviceData.user_id) {
      // User has authorized! Get user and create tokens
      const user = await env.DB.prepare(
        'SELECT id, email, name, picture FROM users WHERE id = ?'
      ).bind(deviceData.user_id).first<{
        id: string;
        email: string;
        name: string;
        picture: string | null;
      }>();

      if (!user) {
        return c.json<ApiResponse>({
          success: false,
          error: 'User not found',
        }, 500);
      }

      // Create tokens
      const { accessToken, refreshToken } = await createTokens(env, user);

      // Store refresh token in KV
      await env.SESSIONS.put(
        `refresh:${user.id}:${refreshToken.slice(-16)}`,
        JSON.stringify({
          createdAt: Date.now(),
          deviceId: deviceData.device_id,
          deviceName: deviceData.device_name,
        }),
        { expirationTtl: 60 * 60 * 24 * 7 } // 7 days
      );

      // Create or update device in devices table
      // Set last_sync to now so device shows as "connected" immediately
      await env.DB.prepare(
        `INSERT INTO devices (id, user_id, name, type, last_sync, created_at)
         VALUES (?, ?, ?, 'karoo', datetime('now'), datetime('now'))
         ON CONFLICT(id) DO UPDATE SET
           name = excluded.name,
           last_sync = datetime('now')`
      ).bind(deviceData.device_id, user.id, deviceData.device_name).run();

      // Delete the device code from D1 (auth complete)
      await env.DB.prepare('DELETE FROM device_codes WHERE device_code = ?')
        .bind(device_code).run();

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
 * Uses D1 database for immediate consistency
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

    // Get device code from D1 by user_code
    const deviceData = await env.DB.prepare(
      `SELECT device_code, device_name, status, expires_at
       FROM device_codes WHERE user_code = ?`
    ).bind(normalizedCode).first<{
      device_code: string;
      device_name: string;
      status: string;
      expires_at: string;
    }>();

    if (!deviceData) {
      return c.json<ApiResponse>({
        success: false,
        error: 'Invalid or expired code',
      }, 400);
    }

    // Check if expired
    if (isExpired(deviceData.expires_at)) {
      // Clean up expired code
      await env.DB.prepare('DELETE FROM device_codes WHERE user_code = ?')
        .bind(normalizedCode).run();

      return c.json<ApiResponse>({
        success: false,
        error: 'Device code expired',
      }, 400);
    }

    // Update status to authorized with user_id
    const updateResult = await env.DB.prepare(
      `UPDATE device_codes SET status = 'authorized', user_id = ? WHERE user_code = ?`
    ).bind(user_id, normalizedCode).run();

    console.log('Authorization UPDATE result:', {
      userCode: normalizedCode,
      userId: user_id.slice(0, 8) + '...',
      changes: updateResult.meta?.changes,
      success: updateResult.success,
    });

    // Check if update actually changed a row
    if (!updateResult.meta?.changes || updateResult.meta.changes === 0) {
      console.error('Authorization UPDATE failed - no rows changed:', { normalizedCode });
      return c.json<ApiResponse>({
        success: false,
        error: 'Failed to authorize device - code not found',
      }, 500);
    }

    // Verify the update worked by reading back
    const verify = await env.DB.prepare(
      `SELECT device_code, status, user_id FROM device_codes WHERE user_code = ?`
    ).bind(normalizedCode).first();

    console.log('Authorization VERIFY:', {
      found: !!verify,
      status: verify?.status,
      hasUserId: !!verify?.user_id,
      deviceCode: verify?.device_code ? String(verify.device_code).slice(0, 8) + '...' : null,
    });

    if (!verify || verify.status !== 'authorized') {
      console.error('Authorization VERIFY failed:', { verify });
      return c.json<ApiResponse>({
        success: false,
        error: 'Authorization verification failed',
      }, 500);
    }

    return c.json<ApiResponse>({
      success: true,
      data: {
        device_name: deviceData.device_name,
        authorized: true,
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
 * Uses D1 database for immediate consistency
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

  // Get device code from D1 by user_code
  const deviceData = await env.DB.prepare(
    `SELECT device_name, status, expires_at FROM device_codes WHERE user_code = ?`
  ).bind(normalizedCode).first<{
    device_name: string;
    status: string;
    expires_at: string;
  }>();

  if (!deviceData) {
    return c.json<ApiResponse>({
      success: false,
      error: 'Invalid or expired code',
    }, 400);
  }

  // Check if expired
  if (isExpired(deviceData.expires_at)) {
    // Clean up expired code
    await env.DB.prepare('DELETE FROM device_codes WHERE user_code = ?')
      .bind(normalizedCode).run();

    return c.json<ApiResponse>({
      success: false,
      error: 'Device code expired',
    }, 400);
  }

  // Check if already authorized
  if (deviceData.status === 'authorized') {
    return c.json<ApiResponse>({
      success: false,
      error: 'Code already used',
    }, 400);
  }

  return c.json<ApiResponse>({
    success: true,
    data: {
      device_name: deviceData.device_name,
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
  const stateId = crypto.randomUUID();

  // Check if there's a custom state (e.g., device_code for linking)
  const customStateParam = c.req.query('state') || '';
  let stateData = '';

  if (customStateParam) {
    try {
      const decoded = JSON.parse(decodeURIComponent(customStateParam));
      // Only allow specific fields, sanitize device_code
      if (decoded.device_code && typeof decoded.device_code === 'string') {
        // Validate device code format: XXXX-XXXX (8 alphanumeric chars with dash)
        const codePattern = /^[A-Z0-9]{4}-[A-Z0-9]{4}$/i;
        if (codePattern.test(decoded.device_code)) {
          stateData = JSON.stringify({ device_code: decoded.device_code.toUpperCase() });
        }
      }
    } catch {
      // Invalid JSON, ignore
    }
  }

  // Store state in KV with 10 min expiry
  await env.SESSIONS.put(`oauth_state:${stateId}`, stateData || '1', { expirationTtl: 600 });

  const state = stateId;

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
  const storedStateData = await env.SESSIONS.get(`oauth_state:${state}`);
  if (!storedStateData) {
    return c.redirect(`${env.APP_URL}/login?error=invalid_state`);
  }
  await env.SESSIONS.delete(`oauth_state:${state}`);

  // Parse stored state data (may contain device_code for linking flow)
  let deviceCode: string | null = null;
  if (storedStateData !== '1') {
    try {
      const parsed = JSON.parse(storedStateData);
      if (parsed.device_code) {
        deviceCode = parsed.device_code;
      }
    } catch {
      // Invalid state data, continue without device code
    }
  }

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

    // If device linking flow, redirect back to link page with tokens
    if (deviceCode) {
      params.set('device_code', deviceCode);
      return c.redirect(`${env.LINK_URL}?${params}`);
    }

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
 * Requires X-Device-ID header to verify device still exists
 */
auth.post('/refresh', async (c) => {
  const env = c.env;

  try {
    const body = await c.req.json<{ refresh_token: string }>();
    const { refresh_token } = body;
    const deviceId = c.req.header('X-Device-ID');

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
    const storedTokenStr = await env.SESSIONS.get(tokenKey);
    if (!storedTokenStr) {
      return c.json<ApiResponse>({ success: false, error: 'Token revoked' }, 401);
    }

    // Parse stored token data
    const storedToken = JSON.parse(storedTokenStr) as {
      createdAt: number;
      deviceId?: string;
      deviceName?: string;
    };

    // If device_id is stored or provided, verify device still exists
    const effectiveDeviceId = deviceId || storedToken.deviceId;
    if (effectiveDeviceId) {
      const device = await env.DB.prepare(
        'SELECT id FROM devices WHERE id = ? AND user_id = ?'
      ).bind(effectiveDeviceId, payload.sub).first();

      if (!device) {
        // Device was removed - revoke this refresh token too
        await env.SESSIONS.delete(tokenKey);
        return c.json<ApiResponse>({
          success: false,
          error: 'Device not found or access revoked',
          code: 'DEVICE_REVOKED',
        }, 403);
      }
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
