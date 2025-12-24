import * as jose from 'jose';
import { Env, JWTPayload } from '../types/env';

const ACCESS_TOKEN_TTL = 15 * 60;        // 15 minutes
const REFRESH_TOKEN_TTL = 7 * 24 * 60 * 60; // 7 days

interface UserForToken {
  id: string;
  email: string;
  name: string;
  picture?: string | null;
}

/**
 * Create access and refresh tokens for a user
 */
export async function createTokens(
  env: Env,
  user: UserForToken
): Promise<{ accessToken: string; refreshToken: string }> {
  const now = Math.floor(Date.now() / 1000);

  // Access token (short-lived)
  const accessToken = await new jose.SignJWT({
    sub: user.id,
    email: user.email,
    name: user.name,
    picture: user.picture || undefined,
  } as JWTPayload)
    .setProtectedHeader({ alg: 'HS256' })
    .setIssuedAt(now)
    .setExpirationTime(now + ACCESS_TOKEN_TTL)
    .setIssuer('kpedal-api')
    .setAudience('kpedal-web')
    .sign(new TextEncoder().encode(env.JWT_ACCESS_SECRET));

  // Refresh token (long-lived)
  const refreshToken = await new jose.SignJWT({
    sub: user.id,
    email: user.email,
    name: user.name,
    picture: user.picture || undefined,
  } as JWTPayload)
    .setProtectedHeader({ alg: 'HS256' })
    .setIssuedAt(now)
    .setExpirationTime(now + REFRESH_TOKEN_TTL)
    .setIssuer('kpedal-api')
    .setAudience('kpedal-refresh')
    .sign(new TextEncoder().encode(env.JWT_REFRESH_SECRET));

  return { accessToken, refreshToken };
}

/**
 * Verify access token and return payload
 */
export async function verifyAccessToken(
  env: Env,
  token: string
): Promise<JWTPayload | null> {
  try {
    const { payload } = await jose.jwtVerify(
      token,
      new TextEncoder().encode(env.JWT_ACCESS_SECRET),
      {
        issuer: 'kpedal-api',
        audience: 'kpedal-web',
      }
    );
    return payload as unknown as JWTPayload;
  } catch (err) {
    return null;
  }
}

/**
 * Verify refresh token and return payload
 */
export async function verifyRefreshToken(
  env: Env,
  token: string
): Promise<JWTPayload | null> {
  try {
    const { payload } = await jose.jwtVerify(
      token,
      new TextEncoder().encode(env.JWT_REFRESH_SECRET),
      {
        issuer: 'kpedal-api',
        audience: 'kpedal-refresh',
      }
    );
    return payload as unknown as JWTPayload;
  } catch (err) {
    return null;
  }
}

/**
 * Revoke a refresh token
 */
export async function revokeRefreshToken(
  env: Env,
  userId: string,
  token: string
): Promise<void> {
  const tokenKey = `refresh:${userId}:${token.slice(-16)}`;
  await env.SESSIONS.delete(tokenKey);
}

/**
 * Revoke all refresh tokens for a user
 */
export async function revokeAllRefreshTokens(
  env: Env,
  userId: string
): Promise<void> {
  // List all keys for this user and delete them
  const list = await env.SESSIONS.list({ prefix: `refresh:${userId}:` });
  for (const key of list.keys) {
    await env.SESSIONS.delete(key.name);
  }
}
