import { Context, Next } from 'hono';
import { Env, JWTPayload, ApiResponse } from '../types/env';
import { verifyAccessToken } from '../auth/jwt';

// Extend Hono's context to include user
declare module 'hono' {
  interface ContextVariableMap {
    user: JWTPayload;
  }
}

/**
 * Authentication middleware
 * Verifies JWT access token from Authorization header
 */
export async function authMiddleware(
  c: Context<{ Bindings: Env }>,
  next: Next
): Promise<Response | void> {
  const authHeader = c.req.header('Authorization');

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return c.json<ApiResponse>(
      { success: false, error: 'Missing or invalid Authorization header' },
      401
    );
  }

  const token = authHeader.slice(7); // Remove "Bearer "

  const payload = await verifyAccessToken(c.env, token);

  if (!payload) {
    return c.json<ApiResponse>(
      { success: false, error: 'Invalid or expired token' },
      401
    );
  }

  // Set user in context for downstream handlers
  c.set('user', payload);

  await next();
}

/**
 * Optional auth middleware
 * Sets user if token is valid, but doesn't block if not
 */
export async function optionalAuthMiddleware(
  c: Context<{ Bindings: Env }>,
  next: Next
): Promise<Response | void> {
  const authHeader = c.req.header('Authorization');

  if (authHeader && authHeader.startsWith('Bearer ')) {
    const token = authHeader.slice(7);
    const payload = await verifyAccessToken(c.env, token);
    if (payload) {
      c.set('user', payload);
    }
  }

  await next();
}
