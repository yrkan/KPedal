import { describe, it, expect, beforeEach, vi } from 'vitest';
import * as jose from 'jose';

/**
 * Unit tests for JWT token utilities
 *
 * Tests token creation, verification, and revocation
 */

// Constants matching jwt.ts
const ACCESS_TOKEN_TTL = 15 * 60;        // 15 minutes
const REFRESH_TOKEN_TTL = 7 * 24 * 60 * 60; // 7 days

// Mock user data
const mockUser = {
  id: 'user-123',
  email: 'test@example.com',
  name: 'Test User',
  picture: 'https://example.com/avatar.png',
};

// Mock secrets
const ACCESS_SECRET = 'test-access-secret-32-chars-long!';
const REFRESH_SECRET = 'test-refresh-secret-32-chars-lng!';

// Mock KV store
class MockKV {
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

  async list(options?: { prefix?: string }): Promise<{ keys: { name: string }[] }> {
    const keys: { name: string }[] = [];
    for (const key of this.store.keys()) {
      if (!options?.prefix || key.startsWith(options.prefix)) {
        keys.push({ name: key });
      }
    }
    return { keys };
  }

  clear(): void {
    this.store.clear();
  }
}

// Token creation function (mirrors jwt.ts)
async function createTokens(
  accessSecret: string,
  refreshSecret: string,
  user: { id: string; email: string; name: string; picture?: string }
): Promise<{ accessToken: string; refreshToken: string }> {
  const now = Math.floor(Date.now() / 1000);

  const accessToken = await new jose.SignJWT({
    sub: user.id,
    email: user.email,
    name: user.name,
    picture: user.picture || undefined,
  })
    .setProtectedHeader({ alg: 'HS256' })
    .setIssuedAt(now)
    .setExpirationTime(now + ACCESS_TOKEN_TTL)
    .setIssuer('kpedal-api')
    .setAudience('kpedal-web')
    .sign(new TextEncoder().encode(accessSecret));

  const refreshToken = await new jose.SignJWT({
    sub: user.id,
    email: user.email,
    name: user.name,
    picture: user.picture || undefined,
  })
    .setProtectedHeader({ alg: 'HS256' })
    .setIssuedAt(now)
    .setExpirationTime(now + REFRESH_TOKEN_TTL)
    .setIssuer('kpedal-api')
    .setAudience('kpedal-refresh')
    .sign(new TextEncoder().encode(refreshSecret));

  return { accessToken, refreshToken };
}

// Token verification functions (mirrors jwt.ts)
async function verifyAccessToken(
  secret: string,
  token: string
): Promise<{ sub: string; email: string; name: string } | null> {
  try {
    const { payload } = await jose.jwtVerify(
      token,
      new TextEncoder().encode(secret),
      {
        issuer: 'kpedal-api',
        audience: 'kpedal-web',
      }
    );
    return payload as unknown as { sub: string; email: string; name: string };
  } catch {
    return null;
  }
}

async function verifyRefreshToken(
  secret: string,
  token: string
): Promise<{ sub: string; email: string; name: string } | null> {
  try {
    const { payload } = await jose.jwtVerify(
      token,
      new TextEncoder().encode(secret),
      {
        issuer: 'kpedal-api',
        audience: 'kpedal-refresh',
      }
    );
    return payload as unknown as { sub: string; email: string; name: string };
  } catch {
    return null;
  }
}

// Revocation functions (mirrors jwt.ts)
async function revokeRefreshToken(
  kv: MockKV,
  userId: string,
  token: string
): Promise<void> {
  const tokenKey = `refresh:${userId}:${token.slice(-16)}`;
  await kv.delete(tokenKey);
}

async function revokeAllRefreshTokens(
  kv: MockKV,
  userId: string
): Promise<void> {
  const list = await kv.list({ prefix: `refresh:${userId}:` });
  for (const key of list.keys) {
    await kv.delete(key.name);
  }
}

async function revokeRefreshTokensForDevice(
  kv: MockKV,
  userId: string,
  deviceId: string
): Promise<number> {
  let deletedCount = 0;
  const list = await kv.list({ prefix: `refresh:${userId}:` });

  for (const key of list.keys) {
    const tokenDataStr = await kv.get(key.name);
    if (tokenDataStr) {
      try {
        const tokenData = JSON.parse(tokenDataStr) as { deviceId?: string };
        if (tokenData.deviceId === deviceId) {
          await kv.delete(key.name);
          deletedCount++;
        }
      } catch {
        // Skip malformed tokens
      }
    }
  }

  return deletedCount;
}

describe('Token Creation', () => {
  it('should create access and refresh tokens', async () => {
    const { accessToken, refreshToken } = await createTokens(
      ACCESS_SECRET,
      REFRESH_SECRET,
      mockUser
    );

    expect(accessToken).toBeDefined();
    expect(refreshToken).toBeDefined();
    expect(accessToken).not.toBe(refreshToken);
  });

  it('should create valid JWT format tokens', async () => {
    const { accessToken, refreshToken } = await createTokens(
      ACCESS_SECRET,
      REFRESH_SECRET,
      mockUser
    );

    // JWT has 3 parts separated by dots
    expect(accessToken.split('.').length).toBe(3);
    expect(refreshToken.split('.').length).toBe(3);
  });

  it('should include user data in access token', async () => {
    const { accessToken } = await createTokens(
      ACCESS_SECRET,
      REFRESH_SECRET,
      mockUser
    );

    const payload = await verifyAccessToken(ACCESS_SECRET, accessToken);

    expect(payload).not.toBeNull();
    expect(payload?.sub).toBe(mockUser.id);
    expect(payload?.email).toBe(mockUser.email);
    expect(payload?.name).toBe(mockUser.name);
  });

  it('should handle user without picture', async () => {
    const userNoPicture = { ...mockUser, picture: undefined };
    const { accessToken } = await createTokens(
      ACCESS_SECRET,
      REFRESH_SECRET,
      userNoPicture
    );

    const payload = await verifyAccessToken(ACCESS_SECRET, accessToken);
    expect(payload).not.toBeNull();
  });
});

describe('Access Token Verification', () => {
  it('should verify valid access token', async () => {
    const { accessToken } = await createTokens(
      ACCESS_SECRET,
      REFRESH_SECRET,
      mockUser
    );

    const payload = await verifyAccessToken(ACCESS_SECRET, accessToken);

    expect(payload).not.toBeNull();
    expect(payload?.sub).toBe(mockUser.id);
  });

  it('should reject access token with wrong secret', async () => {
    const { accessToken } = await createTokens(
      ACCESS_SECRET,
      REFRESH_SECRET,
      mockUser
    );

    const payload = await verifyAccessToken('wrong-secret-32-chars-long!!!!', accessToken);

    expect(payload).toBeNull();
  });

  it('should reject refresh token as access token', async () => {
    const { refreshToken } = await createTokens(
      ACCESS_SECRET,
      REFRESH_SECRET,
      mockUser
    );

    // Refresh token has different audience, should fail access verification
    const payload = await verifyAccessToken(ACCESS_SECRET, refreshToken);

    expect(payload).toBeNull();
  });

  it('should reject malformed token', async () => {
    const payload = await verifyAccessToken(ACCESS_SECRET, 'not.a.valid.token');
    expect(payload).toBeNull();
  });

  it('should reject empty token', async () => {
    const payload = await verifyAccessToken(ACCESS_SECRET, '');
    expect(payload).toBeNull();
  });

  it('should reject expired token', async () => {
    // Create a token that's already expired
    const now = Math.floor(Date.now() / 1000);
    const expiredToken = await new jose.SignJWT({
      sub: mockUser.id,
      email: mockUser.email,
      name: mockUser.name,
    })
      .setProtectedHeader({ alg: 'HS256' })
      .setIssuedAt(now - 3600) // 1 hour ago
      .setExpirationTime(now - 1800) // Expired 30 min ago
      .setIssuer('kpedal-api')
      .setAudience('kpedal-web')
      .sign(new TextEncoder().encode(ACCESS_SECRET));

    const payload = await verifyAccessToken(ACCESS_SECRET, expiredToken);
    expect(payload).toBeNull();
  });
});

describe('Refresh Token Verification', () => {
  it('should verify valid refresh token', async () => {
    const { refreshToken } = await createTokens(
      ACCESS_SECRET,
      REFRESH_SECRET,
      mockUser
    );

    const payload = await verifyRefreshToken(REFRESH_SECRET, refreshToken);

    expect(payload).not.toBeNull();
    expect(payload?.sub).toBe(mockUser.id);
  });

  it('should reject refresh token with wrong secret', async () => {
    const { refreshToken } = await createTokens(
      ACCESS_SECRET,
      REFRESH_SECRET,
      mockUser
    );

    const payload = await verifyRefreshToken('wrong-secret-32-chars-long!!!!', refreshToken);

    expect(payload).toBeNull();
  });

  it('should reject access token as refresh token', async () => {
    const { accessToken } = await createTokens(
      ACCESS_SECRET,
      REFRESH_SECRET,
      mockUser
    );

    // Access token has different audience, should fail refresh verification
    const payload = await verifyRefreshToken(REFRESH_SECRET, accessToken);

    expect(payload).toBeNull();
  });

  it('should have longer TTL than access token', async () => {
    expect(REFRESH_TOKEN_TTL).toBeGreaterThan(ACCESS_TOKEN_TTL);
    expect(REFRESH_TOKEN_TTL).toBe(7 * 24 * 60 * 60); // 7 days
    expect(ACCESS_TOKEN_TTL).toBe(15 * 60); // 15 minutes
  });
});

describe('Token Revocation', () => {
  let kv: MockKV;

  beforeEach(() => {
    kv = new MockKV();
  });

  it('should revoke single refresh token', async () => {
    const token = 'sample-token-1234567890123456';
    const tokenKey = `refresh:${mockUser.id}:${token.slice(-16)}`;

    await kv.put(tokenKey, JSON.stringify({ deviceId: 'device-1' }));
    expect(await kv.get(tokenKey)).not.toBeNull();

    await revokeRefreshToken(kv, mockUser.id, token);

    expect(await kv.get(tokenKey)).toBeNull();
  });

  it('should revoke all refresh tokens for user', async () => {
    // Add multiple tokens for user
    await kv.put(`refresh:${mockUser.id}:token1`, JSON.stringify({ deviceId: 'device-1' }));
    await kv.put(`refresh:${mockUser.id}:token2`, JSON.stringify({ deviceId: 'device-2' }));
    await kv.put(`refresh:${mockUser.id}:token3`, JSON.stringify({ deviceId: 'device-1' }));

    // Add token for different user (should not be affected)
    await kv.put(`refresh:other-user:token1`, JSON.stringify({ deviceId: 'device-1' }));

    await revokeAllRefreshTokens(kv, mockUser.id);

    expect(await kv.get(`refresh:${mockUser.id}:token1`)).toBeNull();
    expect(await kv.get(`refresh:${mockUser.id}:token2`)).toBeNull();
    expect(await kv.get(`refresh:${mockUser.id}:token3`)).toBeNull();
    // Other user's token should still exist
    expect(await kv.get(`refresh:other-user:token1`)).not.toBeNull();
  });

  it('should revoke tokens for specific device', async () => {
    // Add tokens for multiple devices
    await kv.put(`refresh:${mockUser.id}:token1`, JSON.stringify({ deviceId: 'device-1' }));
    await kv.put(`refresh:${mockUser.id}:token2`, JSON.stringify({ deviceId: 'device-2' }));
    await kv.put(`refresh:${mockUser.id}:token3`, JSON.stringify({ deviceId: 'device-1' }));

    const deletedCount = await revokeRefreshTokensForDevice(kv, mockUser.id, 'device-1');

    expect(deletedCount).toBe(2);
    expect(await kv.get(`refresh:${mockUser.id}:token1`)).toBeNull();
    expect(await kv.get(`refresh:${mockUser.id}:token2`)).not.toBeNull(); // Different device
    expect(await kv.get(`refresh:${mockUser.id}:token3`)).toBeNull();
  });

  it('should return 0 when no tokens to revoke', async () => {
    const deletedCount = await revokeRefreshTokensForDevice(kv, mockUser.id, 'nonexistent-device');
    expect(deletedCount).toBe(0);
  });

  it('should handle malformed token data', async () => {
    await kv.put(`refresh:${mockUser.id}:token1`, 'not-json');
    await kv.put(`refresh:${mockUser.id}:token2`, JSON.stringify({ deviceId: 'device-1' }));

    const deletedCount = await revokeRefreshTokensForDevice(kv, mockUser.id, 'device-1');

    expect(deletedCount).toBe(1); // Only the valid token
  });
});

describe('Token Claims', () => {
  it('should include correct issuer in access token', async () => {
    const { accessToken } = await createTokens(
      ACCESS_SECRET,
      REFRESH_SECRET,
      mockUser
    );

    const decoded = jose.decodeJwt(accessToken);
    expect(decoded.iss).toBe('kpedal-api');
  });

  it('should include correct audience in access token', async () => {
    const { accessToken } = await createTokens(
      ACCESS_SECRET,
      REFRESH_SECRET,
      mockUser
    );

    const decoded = jose.decodeJwt(accessToken);
    expect(decoded.aud).toBe('kpedal-web');
  });

  it('should include correct audience in refresh token', async () => {
    const { refreshToken } = await createTokens(
      ACCESS_SECRET,
      REFRESH_SECRET,
      mockUser
    );

    const decoded = jose.decodeJwt(refreshToken);
    expect(decoded.aud).toBe('kpedal-refresh');
  });

  it('should include iat (issued at) claim', async () => {
    const before = Math.floor(Date.now() / 1000);
    const { accessToken } = await createTokens(
      ACCESS_SECRET,
      REFRESH_SECRET,
      mockUser
    );
    const after = Math.floor(Date.now() / 1000);

    const decoded = jose.decodeJwt(accessToken);
    expect(decoded.iat).toBeGreaterThanOrEqual(before);
    expect(decoded.iat).toBeLessThanOrEqual(after);
  });

  it('should include correct exp (expiration) for access token', async () => {
    const before = Math.floor(Date.now() / 1000);
    const { accessToken } = await createTokens(
      ACCESS_SECRET,
      REFRESH_SECRET,
      mockUser
    );

    const decoded = jose.decodeJwt(accessToken);
    expect(decoded.exp).toBe((decoded.iat as number) + ACCESS_TOKEN_TTL);
  });

  it('should include correct exp (expiration) for refresh token', async () => {
    const { refreshToken } = await createTokens(
      ACCESS_SECRET,
      REFRESH_SECRET,
      mockUser
    );

    const decoded = jose.decodeJwt(refreshToken);
    expect(decoded.exp).toBe((decoded.iat as number) + REFRESH_TOKEN_TTL);
  });
});

describe('Security', () => {
  it('should use HS256 algorithm', async () => {
    const { accessToken } = await createTokens(
      ACCESS_SECRET,
      REFRESH_SECRET,
      mockUser
    );

    const header = jose.decodeProtectedHeader(accessToken);
    expect(header.alg).toBe('HS256');
  });

  it('should reject token with wrong issuer', async () => {
    const now = Math.floor(Date.now() / 1000);
    const wrongIssuerToken = await new jose.SignJWT({
      sub: mockUser.id,
    })
      .setProtectedHeader({ alg: 'HS256' })
      .setIssuedAt(now)
      .setExpirationTime(now + ACCESS_TOKEN_TTL)
      .setIssuer('wrong-issuer')
      .setAudience('kpedal-web')
      .sign(new TextEncoder().encode(ACCESS_SECRET));

    const payload = await verifyAccessToken(ACCESS_SECRET, wrongIssuerToken);
    expect(payload).toBeNull();
  });

  it('should reject token with wrong audience', async () => {
    const now = Math.floor(Date.now() / 1000);
    const wrongAudienceToken = await new jose.SignJWT({
      sub: mockUser.id,
    })
      .setProtectedHeader({ alg: 'HS256' })
      .setIssuedAt(now)
      .setExpirationTime(now + ACCESS_TOKEN_TTL)
      .setIssuer('kpedal-api')
      .setAudience('wrong-audience')
      .sign(new TextEncoder().encode(ACCESS_SECRET));

    const payload = await verifyAccessToken(ACCESS_SECRET, wrongAudienceToken);
    expect(payload).toBeNull();
  });

  it('different users should get different tokens', async () => {
    const user1 = { ...mockUser, id: 'user-1' };
    const user2 = { ...mockUser, id: 'user-2' };

    const tokens1 = await createTokens(ACCESS_SECRET, REFRESH_SECRET, user1);
    const tokens2 = await createTokens(ACCESS_SECRET, REFRESH_SECRET, user2);

    expect(tokens1.accessToken).not.toBe(tokens2.accessToken);
    expect(tokens1.refreshToken).not.toBe(tokens2.refreshToken);
  });
});
