/**
 * Cloudflare Worker Environment Bindings
 */
export interface Env {
  // D1 Database
  DB: D1Database;

  // KV Namespace for sessions
  SESSIONS: KVNamespace;

  // Environment variables
  APP_URL: string;
  API_URL: string;
  ENVIRONMENT: 'development' | 'production';

  // Secrets (set via wrangler secret put)
  GOOGLE_CLIENT_ID: string;
  GOOGLE_CLIENT_SECRET: string;
  JWT_ACCESS_SECRET: string;
  JWT_REFRESH_SECRET: string;
}

/**
 * JWT Payload for access tokens
 */
export interface JWTPayload {
  sub: string;        // User ID
  email: string;
  name: string;
  picture?: string;
  iat: number;        // Issued at
  exp: number;        // Expiration
}

/**
 * User from database
 */
export interface User {
  id: string;
  email: string;
  name: string;
  picture: string | null;
  google_id: string;
  created_at: string;
  updated_at: string;
}

/**
 * Ride data from KPedal app
 */
export interface RideData {
  id?: number;
  user_id: string;
  device_id: string;
  timestamp: number;
  duration_ms: number;
  balance_left: number;
  balance_right: number;
  te_left: number;
  te_right: number;
  ps_left: number;
  ps_right: number;
  zone_optimal: number;
  zone_attention: number;
  zone_problem: number;
  score: number;
}

/**
 * API Response wrapper
 */
export interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
}
