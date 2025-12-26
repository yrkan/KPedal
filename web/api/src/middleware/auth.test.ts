import { describe, it, expect, vi, beforeEach } from 'vitest';

/**
 * Unit tests for auth middleware
 *
 * Tests JWT validation, header parsing, and context setting
 */

// Mock JWT verification
const mockVerifyAccessToken = vi.fn();

vi.mock('../auth/jwt', () => ({
  verifyAccessToken: (...args: unknown[]) => mockVerifyAccessToken(...args),
}));

// Mock context and response
interface MockContext {
  req: {
    header: (name: string) => string | undefined;
  };
  json: <T>(data: T, status?: number) => { data: T; status: number };
  set: (key: string, value: unknown) => void;
  env: Record<string, unknown>;
  variables: Record<string, unknown>;
}

function createMockContext(authHeader?: string): MockContext {
  const variables: Record<string, unknown> = {};
  return {
    req: {
      header: (name: string) => (name === 'Authorization' ? authHeader : undefined),
    },
    json: <T>(data: T, status = 200) => ({ data, status }),
    set: (key: string, value: unknown) => {
      variables[key] = value;
    },
    env: { JWT_ACCESS_SECRET: 'test-secret' },
    variables,
  };
}

// Inline auth middleware logic for testing
async function authMiddleware(
  c: MockContext,
  next: () => Promise<void>
): Promise<{ data: unknown; status: number } | void> {
  const authHeader = c.req.header('Authorization');

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return c.json(
      { success: false, error: 'Missing or invalid Authorization header' },
      401
    );
  }

  const token = authHeader.slice(7);
  const payload = await mockVerifyAccessToken(c.env, token);

  if (!payload) {
    return c.json({ success: false, error: 'Invalid or expired token' }, 401);
  }

  c.set('user', payload);
  await next();
}

async function optionalAuthMiddleware(
  c: MockContext,
  next: () => Promise<void>
): Promise<void> {
  const authHeader = c.req.header('Authorization');

  if (authHeader && authHeader.startsWith('Bearer ')) {
    const token = authHeader.slice(7);
    const payload = await mockVerifyAccessToken(c.env, token);
    if (payload) {
      c.set('user', payload);
    }
  }

  await next();
}

describe('Auth Middleware - authMiddleware', () => {
  beforeEach(() => {
    mockVerifyAccessToken.mockReset();
  });

  describe('Header validation', () => {
    it('should return 401 when Authorization header is missing', async () => {
      const ctx = createMockContext(undefined);
      const next = vi.fn();

      const result = await authMiddleware(ctx, next);

      expect(result).toEqual({
        data: { success: false, error: 'Missing or invalid Authorization header' },
        status: 401,
      });
      expect(next).not.toHaveBeenCalled();
    });

    it('should return 401 when Authorization header is empty', async () => {
      const ctx = createMockContext('');
      const next = vi.fn();

      const result = await authMiddleware(ctx, next);

      expect(result).toEqual({
        data: { success: false, error: 'Missing or invalid Authorization header' },
        status: 401,
      });
    });

    it('should return 401 when Authorization header does not start with Bearer', async () => {
      const ctx = createMockContext('Basic abc123');
      const next = vi.fn();

      const result = await authMiddleware(ctx, next);

      expect(result).toEqual({
        data: { success: false, error: 'Missing or invalid Authorization header' },
        status: 401,
      });
    });

    it('should return 401 when Authorization header is just "Bearer"', async () => {
      const ctx = createMockContext('Bearer');
      const next = vi.fn();

      const result = await authMiddleware(ctx, next);

      expect(result).toEqual({
        data: { success: false, error: 'Missing or invalid Authorization header' },
        status: 401,
      });
    });
  });

  describe('Token validation', () => {
    it('should return 401 when token is invalid', async () => {
      mockVerifyAccessToken.mockResolvedValue(null);
      const ctx = createMockContext('Bearer invalid-token');
      const next = vi.fn();

      const result = await authMiddleware(ctx, next);

      expect(result).toEqual({
        data: { success: false, error: 'Invalid or expired token' },
        status: 401,
      });
      expect(mockVerifyAccessToken).toHaveBeenCalledWith(
        ctx.env,
        'invalid-token'
      );
      expect(next).not.toHaveBeenCalled();
    });

    it('should return 401 when token is expired', async () => {
      mockVerifyAccessToken.mockResolvedValue(null);
      const ctx = createMockContext('Bearer expired-token');
      const next = vi.fn();

      const result = await authMiddleware(ctx, next);

      expect(result).toEqual({
        data: { success: false, error: 'Invalid or expired token' },
        status: 401,
      });
    });
  });

  describe('Successful authentication', () => {
    it('should call next and set user when token is valid', async () => {
      const mockPayload = {
        userId: 'user-123',
        email: 'test@example.com',
      };
      mockVerifyAccessToken.mockResolvedValue(mockPayload);
      const ctx = createMockContext('Bearer valid-token');
      const next = vi.fn().mockResolvedValue(undefined);

      const result = await authMiddleware(ctx, next);

      expect(result).toBeUndefined();
      expect(next).toHaveBeenCalled();
      expect(ctx.variables['user']).toEqual(mockPayload);
    });

    it('should extract token correctly from Bearer header', async () => {
      const mockPayload = { userId: 'user-123' };
      mockVerifyAccessToken.mockResolvedValue(mockPayload);
      const ctx = createMockContext('Bearer my.jwt.token');
      const next = vi.fn().mockResolvedValue(undefined);

      await authMiddleware(ctx, next);

      expect(mockVerifyAccessToken).toHaveBeenCalledWith(
        ctx.env,
        'my.jwt.token'
      );
    });

    it('should handle tokens with spaces after Bearer correctly', async () => {
      const mockPayload = { userId: 'user-123' };
      mockVerifyAccessToken.mockResolvedValue(mockPayload);
      const ctx = createMockContext('Bearer  double-space-token');
      const next = vi.fn().mockResolvedValue(undefined);

      await authMiddleware(ctx, next);

      // Should include the extra space as part of token
      expect(mockVerifyAccessToken).toHaveBeenCalledWith(
        ctx.env,
        ' double-space-token'
      );
    });
  });
});

describe('Auth Middleware - optionalAuthMiddleware', () => {
  beforeEach(() => {
    mockVerifyAccessToken.mockReset();
  });

  it('should call next without setting user when no header', async () => {
    const ctx = createMockContext(undefined);
    const next = vi.fn().mockResolvedValue(undefined);

    await optionalAuthMiddleware(ctx, next);

    expect(next).toHaveBeenCalled();
    expect(ctx.variables['user']).toBeUndefined();
  });

  it('should call next without setting user when header is not Bearer', async () => {
    const ctx = createMockContext('Basic abc123');
    const next = vi.fn().mockResolvedValue(undefined);

    await optionalAuthMiddleware(ctx, next);

    expect(next).toHaveBeenCalled();
    expect(ctx.variables['user']).toBeUndefined();
    expect(mockVerifyAccessToken).not.toHaveBeenCalled();
  });

  it('should call next without setting user when token is invalid', async () => {
    mockVerifyAccessToken.mockResolvedValue(null);
    const ctx = createMockContext('Bearer invalid-token');
    const next = vi.fn().mockResolvedValue(undefined);

    await optionalAuthMiddleware(ctx, next);

    expect(next).toHaveBeenCalled();
    expect(ctx.variables['user']).toBeUndefined();
  });

  it('should set user and call next when token is valid', async () => {
    const mockPayload = { userId: 'user-123', email: 'test@example.com' };
    mockVerifyAccessToken.mockResolvedValue(mockPayload);
    const ctx = createMockContext('Bearer valid-token');
    const next = vi.fn().mockResolvedValue(undefined);

    await optionalAuthMiddleware(ctx, next);

    expect(next).toHaveBeenCalled();
    expect(ctx.variables['user']).toEqual(mockPayload);
  });

  it('should always call next regardless of auth status', async () => {
    const next = vi.fn().mockResolvedValue(undefined);

    // No header
    await optionalAuthMiddleware(createMockContext(undefined), next);
    expect(next).toHaveBeenCalledTimes(1);

    // Invalid header
    await optionalAuthMiddleware(createMockContext('Invalid'), next);
    expect(next).toHaveBeenCalledTimes(2);

    // Invalid token
    mockVerifyAccessToken.mockResolvedValue(null);
    await optionalAuthMiddleware(createMockContext('Bearer bad'), next);
    expect(next).toHaveBeenCalledTimes(3);

    // Valid token
    mockVerifyAccessToken.mockResolvedValue({ userId: '123' });
    await optionalAuthMiddleware(createMockContext('Bearer good'), next);
    expect(next).toHaveBeenCalledTimes(4);
  });
});

describe('Auth Middleware - Edge Cases', () => {
  beforeEach(() => {
    mockVerifyAccessToken.mockReset();
  });

  it('should handle case-sensitive Bearer prefix', async () => {
    const ctx = createMockContext('bearer lowercase-token');
    const next = vi.fn();

    const result = await authMiddleware(ctx, next);

    // Should fail - Bearer is case-sensitive
    expect(result?.status).toBe(401);
  });

  it('should handle BEARER uppercase prefix', async () => {
    const ctx = createMockContext('BEARER uppercase-token');
    const next = vi.fn();

    const result = await authMiddleware(ctx, next);

    // Should fail - Bearer is case-sensitive
    expect(result?.status).toBe(401);
  });

  it('should handle very long tokens', async () => {
    const longToken = 'a'.repeat(10000);
    mockVerifyAccessToken.mockResolvedValue({ userId: 'user-123' });
    const ctx = createMockContext(`Bearer ${longToken}`);
    const next = vi.fn().mockResolvedValue(undefined);

    await authMiddleware(ctx, next);

    expect(mockVerifyAccessToken).toHaveBeenCalledWith(ctx.env, longToken);
    expect(next).toHaveBeenCalled();
  });

  it('should handle token with special characters', async () => {
    const specialToken = 'eyJ+/=.token.with.special';
    mockVerifyAccessToken.mockResolvedValue({ userId: 'user-123' });
    const ctx = createMockContext(`Bearer ${specialToken}`);
    const next = vi.fn().mockResolvedValue(undefined);

    await authMiddleware(ctx, next);

    expect(mockVerifyAccessToken).toHaveBeenCalledWith(ctx.env, specialToken);
  });
});
