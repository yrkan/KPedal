import { Hono } from 'hono';
import { cors } from 'hono/cors';
import { logger } from 'hono/logger';
import { secureHeaders } from 'hono/secure-headers';
import { Env, ApiResponse } from './types/env';
import { authRoutes } from './auth/routes';
import { ridesRoutes } from './api/rides';
import { syncRoutes } from './api/sync';
import { authMiddleware } from './middleware/auth';

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

// Auth routes (login, callback, refresh)
app.route('/auth', authRoutes);

// ============================================
// Protected Routes (require auth)
// ============================================

// Apply auth middleware to /api/*
app.use('/api/*', authMiddleware);

// User rides
app.route('/api/rides', ridesRoutes);

// Device sync (from Karoo)
app.route('/api/sync', syncRoutes);

// User profile
app.get('/api/me', async (c) => {
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
