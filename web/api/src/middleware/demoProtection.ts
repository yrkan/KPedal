import { Context, Next } from 'hono';
import { Env, ApiResponse, isDemoUser } from '../types/env';

/**
 * Demo account protection middleware
 * Blocks write operations (DELETE, PUT, POST) for demo account
 * Demo account is read-only for viewing purposes
 */
export async function demoProtectionMiddleware(
  c: Context<{ Bindings: Env }>,
  next: Next
): Promise<Response | void> {
  const user = c.get('user');

  // Only apply to demo user
  if (!user || !isDemoUser(user.sub)) {
    return next();
  }

  const method = c.req.method;
  const path = c.req.path;

  // Allow GET requests (read-only)
  if (method === 'GET') {
    return next();
  }

  // Block all write operations for demo account
  // DELETE: rides, devices
  // PUT: settings
  // POST: sync (can't add new rides)

  const blockedOperations = [
    { method: 'DELETE', paths: ['/rides/', '/devices/'] },
    { method: 'PUT', paths: ['/settings', '/rides/'] },
    { method: 'POST', paths: ['/sync/', '/devices/'] },
  ];

  for (const op of blockedOperations) {
    if (method === op.method) {
      for (const blockedPath of op.paths) {
        if (path.includes(blockedPath) || path === blockedPath.replace('/', '')) {
          return c.json<ApiResponse>(
            {
              success: false,
              error: 'Demo account is read-only. Sign in with Google for full access.',
              code: 'DEMO_READ_ONLY',
            },
            403
          );
        }
      }
    }
  }

  // Allow other requests
  return next();
}
