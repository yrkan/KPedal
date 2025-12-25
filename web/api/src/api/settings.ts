import { Hono } from 'hono';
import { Env, ApiResponse, UserSettings } from '../types/env';

const settings = new Hono<{ Bindings: Env }>();

// Default settings values
const DEFAULT_SETTINGS: UserSettings = {
  balance_threshold: 5,
  te_optimal_min: 70,
  te_optimal_max: 80,
  ps_minimum: 20,
  alerts_enabled: true,
  screen_wake_on_alert: true,
  balance_alert_enabled: true,
  balance_alert_trigger: 'PROBLEM_ONLY',
  balance_alert_visual: true,
  balance_alert_sound: false,
  balance_alert_vibration: true,
  balance_alert_cooldown: 30,
  te_alert_enabled: true,
  te_alert_trigger: 'PROBLEM_ONLY',
  te_alert_visual: true,
  te_alert_sound: false,
  te_alert_vibration: true,
  te_alert_cooldown: 30,
  ps_alert_enabled: true,
  ps_alert_trigger: 'PROBLEM_ONLY',
  ps_alert_visual: true,
  ps_alert_sound: false,
  ps_alert_vibration: true,
  ps_alert_cooldown: 30,
  background_mode_enabled: true,
  auto_sync_enabled: true,
};

// Database row type (uses 0/1 for booleans)
interface SettingsRow {
  user_id: string;
  balance_threshold: number;
  te_optimal_min: number;
  te_optimal_max: number;
  ps_minimum: number;
  alerts_enabled: number;
  screen_wake_on_alert: number;
  balance_alert_enabled: number;
  balance_alert_trigger: string;
  balance_alert_visual: number;
  balance_alert_sound: number;
  balance_alert_vibration: number;
  balance_alert_cooldown: number;
  te_alert_enabled: number;
  te_alert_trigger: string;
  te_alert_visual: number;
  te_alert_sound: number;
  te_alert_vibration: number;
  te_alert_cooldown: number;
  ps_alert_enabled: number;
  ps_alert_trigger: string;
  ps_alert_visual: number;
  ps_alert_sound: number;
  ps_alert_vibration: number;
  ps_alert_cooldown: number;
  background_mode_enabled: number;
  auto_sync_enabled: number;
  updated_at: string;
}

// Convert DB row to UserSettings
function rowToSettings(row: SettingsRow): UserSettings {
  return {
    balance_threshold: row.balance_threshold,
    te_optimal_min: row.te_optimal_min,
    te_optimal_max: row.te_optimal_max,
    ps_minimum: row.ps_minimum,
    alerts_enabled: row.alerts_enabled === 1,
    screen_wake_on_alert: row.screen_wake_on_alert === 1,
    balance_alert_enabled: row.balance_alert_enabled === 1,
    balance_alert_trigger: row.balance_alert_trigger as 'PROBLEM_ONLY' | 'ATTENTION_AND_PROBLEM',
    balance_alert_visual: row.balance_alert_visual === 1,
    balance_alert_sound: row.balance_alert_sound === 1,
    balance_alert_vibration: row.balance_alert_vibration === 1,
    balance_alert_cooldown: row.balance_alert_cooldown,
    te_alert_enabled: row.te_alert_enabled === 1,
    te_alert_trigger: row.te_alert_trigger as 'PROBLEM_ONLY' | 'ATTENTION_AND_PROBLEM',
    te_alert_visual: row.te_alert_visual === 1,
    te_alert_sound: row.te_alert_sound === 1,
    te_alert_vibration: row.te_alert_vibration === 1,
    te_alert_cooldown: row.te_alert_cooldown,
    ps_alert_enabled: row.ps_alert_enabled === 1,
    ps_alert_trigger: row.ps_alert_trigger as 'PROBLEM_ONLY' | 'ATTENTION_AND_PROBLEM',
    ps_alert_visual: row.ps_alert_visual === 1,
    ps_alert_sound: row.ps_alert_sound === 1,
    ps_alert_vibration: row.ps_alert_vibration === 1,
    ps_alert_cooldown: row.ps_alert_cooldown,
    background_mode_enabled: row.background_mode_enabled === 1,
    auto_sync_enabled: row.auto_sync_enabled === 1,
    updated_at: row.updated_at,
  };
}

// Validate and clamp settings values
function validateSettings(input: Partial<UserSettings>): Partial<UserSettings> {
  const validated: Partial<UserSettings> = {};

  // Threshold settings with ranges
  if (input.balance_threshold !== undefined) {
    validated.balance_threshold = Math.max(1, Math.min(10, input.balance_threshold));
  }
  if (input.te_optimal_min !== undefined) {
    validated.te_optimal_min = Math.max(50, Math.min(90, input.te_optimal_min));
  }
  if (input.te_optimal_max !== undefined) {
    const min = validated.te_optimal_min ?? input.te_optimal_min ?? 70;
    validated.te_optimal_max = Math.max(min + 5, Math.min(100, input.te_optimal_max));
  }
  if (input.ps_minimum !== undefined) {
    validated.ps_minimum = Math.max(10, Math.min(30, input.ps_minimum));
  }

  // Boolean settings
  const booleanKeys: (keyof UserSettings)[] = [
    'alerts_enabled', 'screen_wake_on_alert',
    'balance_alert_enabled', 'balance_alert_visual', 'balance_alert_sound', 'balance_alert_vibration',
    'te_alert_enabled', 'te_alert_visual', 'te_alert_sound', 'te_alert_vibration',
    'ps_alert_enabled', 'ps_alert_visual', 'ps_alert_sound', 'ps_alert_vibration',
    'background_mode_enabled', 'auto_sync_enabled',
  ];
  for (const key of booleanKeys) {
    if (input[key] !== undefined) {
      (validated as any)[key] = Boolean(input[key]);
    }
  }

  // Trigger levels
  const validTriggers = ['PROBLEM_ONLY', 'ATTENTION_AND_PROBLEM'];
  if (input.balance_alert_trigger !== undefined && validTriggers.includes(input.balance_alert_trigger)) {
    validated.balance_alert_trigger = input.balance_alert_trigger;
  }
  if (input.te_alert_trigger !== undefined && validTriggers.includes(input.te_alert_trigger)) {
    validated.te_alert_trigger = input.te_alert_trigger;
  }
  if (input.ps_alert_trigger !== undefined && validTriggers.includes(input.ps_alert_trigger)) {
    validated.ps_alert_trigger = input.ps_alert_trigger;
  }

  // Cooldowns
  const cooldownKeys: (keyof UserSettings)[] = [
    'balance_alert_cooldown', 'te_alert_cooldown', 'ps_alert_cooldown',
  ];
  for (const key of cooldownKeys) {
    if (input[key] !== undefined) {
      (validated as any)[key] = Math.max(10, Math.min(120, input[key] as number));
    }
  }

  return validated;
}

/**
 * GET /api/settings
 * Get user settings (returns defaults if none saved)
 */
settings.get('/', async (c) => {
  const user = c.get('user');

  try {
    const row = await c.env.DB
      .prepare('SELECT * FROM user_settings WHERE user_id = ?')
      .bind(user.sub)
      .first<SettingsRow>();

    if (row) {
      return c.json<ApiResponse<{ settings: UserSettings }>>({
        success: true,
        data: { settings: rowToSettings(row) },
      });
    }

    // Return defaults if no settings saved
    return c.json<ApiResponse<{ settings: UserSettings }>>({
      success: true,
      data: { settings: DEFAULT_SETTINGS },
    });
  } catch (err) {
    console.error('Error fetching settings:', err);
    return c.json<ApiResponse>({ success: false, error: 'Failed to fetch settings' }, 500);
  }
});

/**
 * PUT /api/settings
 * Update user settings (partial update supported)
 */
settings.put('/', async (c) => {
  const user = c.get('user');

  try {
    const body = await c.req.json<Partial<UserSettings>>();
    const validated = validateSettings(body);

    if (Object.keys(validated).length === 0) {
      return c.json<ApiResponse>({ success: false, error: 'No valid settings provided' }, 400);
    }

    // Check if settings exist
    const existing = await c.env.DB
      .prepare('SELECT user_id FROM user_settings WHERE user_id = ?')
      .bind(user.sub)
      .first();

    if (existing) {
      // Build UPDATE query dynamically
      const updates: string[] = [];
      const values: (string | number)[] = [];

      for (const [key, value] of Object.entries(validated)) {
        updates.push(`${key} = ?`);
        values.push(typeof value === 'boolean' ? (value ? 1 : 0) : value);
      }
      updates.push("updated_at = datetime('now')");
      values.push(user.sub);

      await c.env.DB
        .prepare(`UPDATE user_settings SET ${updates.join(', ')} WHERE user_id = ?`)
        .bind(...values)
        .run();
    } else {
      // Insert with defaults for missing fields
      const merged = { ...DEFAULT_SETTINGS, ...validated };

      await c.env.DB
        .prepare(`
          INSERT INTO user_settings (
            user_id, balance_threshold, te_optimal_min, te_optimal_max, ps_minimum,
            alerts_enabled, screen_wake_on_alert,
            balance_alert_enabled, balance_alert_trigger, balance_alert_visual,
            balance_alert_sound, balance_alert_vibration, balance_alert_cooldown,
            te_alert_enabled, te_alert_trigger, te_alert_visual,
            te_alert_sound, te_alert_vibration, te_alert_cooldown,
            ps_alert_enabled, ps_alert_trigger, ps_alert_visual,
            ps_alert_sound, ps_alert_vibration, ps_alert_cooldown,
            background_mode_enabled, auto_sync_enabled, updated_at
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
        `)
        .bind(
          user.sub,
          merged.balance_threshold,
          merged.te_optimal_min,
          merged.te_optimal_max,
          merged.ps_minimum,
          merged.alerts_enabled ? 1 : 0,
          merged.screen_wake_on_alert ? 1 : 0,
          merged.balance_alert_enabled ? 1 : 0,
          merged.balance_alert_trigger,
          merged.balance_alert_visual ? 1 : 0,
          merged.balance_alert_sound ? 1 : 0,
          merged.balance_alert_vibration ? 1 : 0,
          merged.balance_alert_cooldown,
          merged.te_alert_enabled ? 1 : 0,
          merged.te_alert_trigger,
          merged.te_alert_visual ? 1 : 0,
          merged.te_alert_sound ? 1 : 0,
          merged.te_alert_vibration ? 1 : 0,
          merged.te_alert_cooldown,
          merged.ps_alert_enabled ? 1 : 0,
          merged.ps_alert_trigger,
          merged.ps_alert_visual ? 1 : 0,
          merged.ps_alert_sound ? 1 : 0,
          merged.ps_alert_vibration ? 1 : 0,
          merged.ps_alert_cooldown,
          merged.background_mode_enabled ? 1 : 0,
          merged.auto_sync_enabled ? 1 : 0,
        )
        .run();
    }

    // Fetch updated settings
    const row = await c.env.DB
      .prepare('SELECT * FROM user_settings WHERE user_id = ?')
      .bind(user.sub)
      .first<SettingsRow>();

    return c.json<ApiResponse<{ settings: UserSettings }>>({
      success: true,
      data: { settings: row ? rowToSettings(row) : DEFAULT_SETTINGS },
      message: 'Settings updated',
    });
  } catch (err) {
    console.error('Error updating settings:', err);
    return c.json<ApiResponse>({ success: false, error: 'Failed to update settings' }, 500);
  }
});

export { settings as settingsRoutes };
