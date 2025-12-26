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
  LINK_URL: string;
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
  user_id?: string;
  device_id?: string;
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
  // Extended metrics (optional, defaults to 0)
  power_avg?: number;
  power_max?: number;
  cadence_avg?: number;
  hr_avg?: number;
  hr_max?: number;
  speed_avg?: number;
  distance_km?: number;
  // Pro cyclist metrics (optional, defaults to 0)
  elevation_gain?: number;
  elevation_loss?: number;
  grade_avg?: number;
  grade_max?: number;
  normalized_power?: number;
  energy_kj?: number;
}

/**
 * API Response wrapper
 */
export interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
  code?: string;  // Error code for specific handling (e.g., 'DEVICE_REVOKED')
}

/**
 * User settings (synced between devices and web)
 */
export interface UserSettings {
  // Threshold settings
  balance_threshold: number;
  te_optimal_min: number;
  te_optimal_max: number;
  ps_minimum: number;

  // Alert settings - Global
  alerts_enabled: boolean;
  screen_wake_on_alert: boolean;

  // Alert settings - Balance
  balance_alert_enabled: boolean;
  balance_alert_trigger: 'PROBLEM_ONLY' | 'ATTENTION_AND_PROBLEM';
  balance_alert_visual: boolean;
  balance_alert_sound: boolean;
  balance_alert_vibration: boolean;
  balance_alert_cooldown: number;

  // Alert settings - TE
  te_alert_enabled: boolean;
  te_alert_trigger: 'PROBLEM_ONLY' | 'ATTENTION_AND_PROBLEM';
  te_alert_visual: boolean;
  te_alert_sound: boolean;
  te_alert_vibration: boolean;
  te_alert_cooldown: number;

  // Alert settings - PS
  ps_alert_enabled: boolean;
  ps_alert_trigger: 'PROBLEM_ONLY' | 'ATTENTION_AND_PROBLEM';
  ps_alert_visual: boolean;
  ps_alert_sound: boolean;
  ps_alert_vibration: boolean;
  ps_alert_cooldown: number;

  // Sync settings
  background_mode_enabled: boolean;
  auto_sync_enabled: boolean;

  // Metadata
  updated_at?: string;
}
