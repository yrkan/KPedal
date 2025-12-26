import { Hono } from 'hono';
import { cors } from 'hono/cors';
import { logger } from 'hono/logger';
import { secureHeaders } from 'hono/secure-headers';
import { Env, ApiResponse } from './types/env';
import { authRoutes } from './auth/routes';
import { ridesRoutes } from './api/rides';
import { syncRoutes } from './api/sync';
import { devicesRoutes } from './api/devices';
import { settingsRoutes } from './api/settings';
import { drillRoutes } from './api/drills';
import { achievementRoutes } from './api/achievements';
import { authMiddleware } from './middleware/auth';
import { authRateLimit, apiRateLimit, syncRateLimit } from './middleware/rateLimit';

const app = new Hono<{ Bindings: Env }>();

// ============================================
// Global Middleware
// ============================================

// Logging
app.use('*', logger());

// Security headers
app.use('*', secureHeaders());

// CORS - allow frontend
app.use('*', cors({
  origin: (origin, c) => {
    const env = c.env;
    const allowedOrigins = [
      env.APP_URL,
      env.LINK_URL,
      'https://kpedal.com',     // Main domain (serves same app)
      'http://localhost:5173',  // SvelteKit dev
      'http://localhost:4173',  // SvelteKit preview
    ];
    return allowedOrigins.includes(origin) ? origin : env.APP_URL;
  },
  allowMethods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowHeaders: ['Content-Type', 'Authorization', 'X-Device-ID'],
  credentials: true,
  maxAge: 86400,
}));

// ============================================
// Health Check
// ============================================

app.get('/', (c) => {
  const response: ApiResponse = {
    success: true,
    message: 'KPedal API v1.0.0',
    data: {
      status: 'healthy',
      timestamp: new Date().toISOString(),
    },
  };
  return c.json(response);
});

app.get('/health', (c) => {
  return c.json({ status: 'ok', timestamp: Date.now() });
});

// ============================================
// Public Routes
// ============================================

// Auth routes (login, callback, refresh) with stricter rate limit
app.use('/auth/*', authRateLimit);
app.route('/auth', authRoutes);

// ============================================
// Protected Routes (require auth)
// ============================================

// Apply auth middleware to protected routes
// Note: need both '/path' and '/path/*' for Hono middleware matching
app.use('/rides', authMiddleware);
app.use('/rides/*', authMiddleware);
app.use('/devices', authMiddleware);
app.use('/devices/*', authMiddleware);
app.use('/settings', authMiddleware);
app.use('/drills', authMiddleware);
app.use('/drills/*', authMiddleware);
app.use('/achievements', authMiddleware);
app.use('/achievements/*', authMiddleware);
app.use('/sync/*', authMiddleware);
app.use('/me', authMiddleware);

// Apply rate limiting to protected routes
app.use('/rides', apiRateLimit);
app.use('/rides/*', apiRateLimit);
app.use('/devices', apiRateLimit);
app.use('/devices/*', apiRateLimit);
app.use('/settings', apiRateLimit);
app.use('/drills', apiRateLimit);
app.use('/drills/*', apiRateLimit);
app.use('/achievements', apiRateLimit);
app.use('/achievements/*', apiRateLimit);

// User rides
app.route('/rides', ridesRoutes);

// User devices
app.route('/devices', devicesRoutes);

// User settings
app.route('/settings', settingsRoutes);

// User drills
app.route('/drills', drillRoutes);

// User achievements
app.route('/achievements', achievementRoutes);

// Device sync (from Karoo) with sync-specific rate limit
app.use('/sync/*', syncRateLimit);
app.route('/sync', syncRoutes);

// User profile
app.get('/me', async (c) => {
  const user = c.get('user');
  const response: ApiResponse = {
    success: true,
    data: {
      id: user.sub,
      email: user.email,
      name: user.name,
      picture: user.picture,
    },
  };
  return c.json(response);
});

// ============================================
// Error Handling
// ============================================

app.onError((err, c) => {
  console.error('Unhandled error:', err);
  const response: ApiResponse = {
    success: false,
    error: c.env.ENVIRONMENT === 'development' ? err.message : 'Internal server error',
  };
  return c.json(response, 500);
});

app.notFound((c) => {
  const response: ApiResponse = {
    success: false,
    error: 'Not found',
  };
  return c.json(response, 404);
});

export default app;
