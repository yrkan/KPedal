import { describe, it, expect, beforeEach } from 'vitest';

/**
 * Unit tests for Settings API
 *
 * Tests the settings endpoints: get, put (partial update)
 * Also tests validation and default values
 */

// Types matching the API
interface UserSettings {
  balance_threshold: number;
  te_optimal_min: number;
  te_optimal_max: number;
  ps_minimum: number;
  alerts_enabled: boolean;
  screen_wake_on_alert: boolean;
  balance_alert_enabled: boolean;
  balance_alert_trigger: 'PROBLEM_ONLY' | 'ATTENTION_AND_PROBLEM';
  balance_alert_visual: boolean;
  balance_alert_sound: boolean;
  balance_alert_vibration: boolean;
  balance_alert_cooldown: number;
  te_alert_enabled: boolean;
  te_alert_trigger: 'PROBLEM_ONLY' | 'ATTENTION_AND_PROBLEM';
  te_alert_visual: boolean;
  te_alert_sound: boolean;
  te_alert_vibration: boolean;
  te_alert_cooldown: number;
  ps_alert_enabled: boolean;
  ps_alert_trigger: 'PROBLEM_ONLY' | 'ATTENTION_AND_PROBLEM';
  ps_alert_visual: boolean;
  ps_alert_sound: boolean;
  ps_alert_vibration: boolean;
  ps_alert_cooldown: number;
  background_mode_enabled: boolean;
  auto_sync_enabled: boolean;
  updated_at?: string;
}

// Default settings (matches API)
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

// Mock database for settings
class MockSettingsDatabase {
  private settings: Map<string, UserSettings> = new Map();

  getSettings(userId: string): UserSettings | null {
    return this.settings.get(userId) || null;
  }

  saveSettings(userId: string, settings: UserSettings): void {
    this.settings.set(userId, {
      ...settings,
      updated_at: new Date().toISOString(),
    });
  }

  updateSettings(userId: string, partial: Partial<UserSettings>): UserSettings {
    const existing = this.settings.get(userId) || { ...DEFAULT_SETTINGS };
    const updated = {
      ...existing,
      ...partial,
      updated_at: new Date().toISOString(),
    };
    this.settings.set(userId, updated);
    return updated;
  }

  hasSettings(userId: string): boolean {
    return this.settings.has(userId);
  }

  clear() {
    this.settings.clear();
  }
}

// Validation function (matches API implementation)
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

// API handlers
function handleGetSettings(
  db: MockSettingsDatabase,
  userId: string
): { success: boolean; data: { settings: UserSettings } } {
  const settings = db.getSettings(userId);
  return {
    success: true,
    data: { settings: settings || DEFAULT_SETTINGS },
  };
}

function handlePutSettings(
  db: MockSettingsDatabase,
  userId: string,
  body: Partial<UserSettings>
): { success: boolean; status: number; data?: { settings: UserSettings }; error?: string; message?: string } {
  const validated = validateSettings(body);

  if (Object.keys(validated).length === 0) {
    return { success: false, status: 400, error: 'No valid settings provided' };
  }

  const updated = db.updateSettings(userId, validated);
  return {
    success: true,
    status: 200,
    data: { settings: updated },
    message: 'Settings updated',
  };
}

describe('Get Settings Endpoint', () => {
  let db: MockSettingsDatabase;

  beforeEach(() => {
    db = new MockSettingsDatabase();
  });

  it('should return defaults for new user', () => {
    const result = handleGetSettings(db, 'user-1');

    expect(result.success).toBe(true);
    expect(result.data.settings).toEqual(DEFAULT_SETTINGS);
  });

  it('should return saved settings for existing user', () => {
    const customSettings: UserSettings = {
      ...DEFAULT_SETTINGS,
      balance_threshold: 3,
      alerts_enabled: false,
    };
    db.saveSettings('user-1', customSettings);

    const result = handleGetSettings(db, 'user-1');

    expect(result.data.settings.balance_threshold).toBe(3);
    expect(result.data.settings.alerts_enabled).toBe(false);
  });

  it('should return settings with updated_at', () => {
    db.saveSettings('user-1', { ...DEFAULT_SETTINGS });

    const result = handleGetSettings(db, 'user-1');

    expect(result.data.settings.updated_at).toBeDefined();
  });
});

describe('Put Settings Endpoint', () => {
  let db: MockSettingsDatabase;

  beforeEach(() => {
    db = new MockSettingsDatabase();
  });

  it('should create settings for new user', () => {
    const result = handlePutSettings(db, 'user-1', { balance_threshold: 3 });

    expect(result.success).toBe(true);
    expect(result.status).toBe(200);
    expect(result.data?.settings.balance_threshold).toBe(3);
    expect(db.hasSettings('user-1')).toBe(true);
  });

  it('should update existing settings', () => {
    db.saveSettings('user-1', { ...DEFAULT_SETTINGS });

    const result = handlePutSettings(db, 'user-1', { alerts_enabled: false });

    expect(result.data?.settings.alerts_enabled).toBe(false);
    // Other settings preserved
    expect(result.data?.settings.balance_threshold).toBe(5);
  });

  it('should support partial updates', () => {
    db.saveSettings('user-1', { ...DEFAULT_SETTINGS });

    handlePutSettings(db, 'user-1', { balance_threshold: 3 });
    const settings = db.getSettings('user-1')!;

    expect(settings.balance_threshold).toBe(3);
    expect(settings.te_optimal_min).toBe(70); // Unchanged
    expect(settings.alerts_enabled).toBe(true); // Unchanged
  });

  it('should reject empty update', () => {
    const result = handlePutSettings(db, 'user-1', {});

    expect(result.success).toBe(false);
    expect(result.status).toBe(400);
    expect(result.error).toBe('No valid settings provided');
  });

  it('should update updated_at timestamp', () => {
    db.saveSettings('user-1', { ...DEFAULT_SETTINGS, updated_at: '2024-01-01T00:00:00Z' });

    handlePutSettings(db, 'user-1', { balance_threshold: 3 });
    const settings = db.getSettings('user-1')!;

    expect(settings.updated_at).not.toBe('2024-01-01T00:00:00Z');
  });
});

describe('Settings Validation - Thresholds', () => {
  it('should clamp balance_threshold to 1-10', () => {
    expect(validateSettings({ balance_threshold: 0 }).balance_threshold).toBe(1);
    expect(validateSettings({ balance_threshold: 5 }).balance_threshold).toBe(5);
    expect(validateSettings({ balance_threshold: 15 }).balance_threshold).toBe(10);
  });

  it('should clamp te_optimal_min to 50-90', () => {
    expect(validateSettings({ te_optimal_min: 40 }).te_optimal_min).toBe(50);
    expect(validateSettings({ te_optimal_min: 70 }).te_optimal_min).toBe(70);
    expect(validateSettings({ te_optimal_min: 95 }).te_optimal_min).toBe(90);
  });

  it('should clamp te_optimal_max to min+5 to 100', () => {
    // With te_optimal_min = 70 (default), max must be >= 75
    expect(validateSettings({ te_optimal_max: 60 }).te_optimal_max).toBe(75);
    expect(validateSettings({ te_optimal_max: 80 }).te_optimal_max).toBe(80);
    expect(validateSettings({ te_optimal_max: 110 }).te_optimal_max).toBe(100);
  });

  it('should ensure te_optimal_max > te_optimal_min when both provided', () => {
    const result = validateSettings({ te_optimal_min: 80, te_optimal_max: 75 });
    expect(result.te_optimal_min).toBe(80);
    expect(result.te_optimal_max).toBe(85); // min + 5
  });

  it('should clamp ps_minimum to 10-30', () => {
    expect(validateSettings({ ps_minimum: 5 }).ps_minimum).toBe(10);
    expect(validateSettings({ ps_minimum: 20 }).ps_minimum).toBe(20);
    expect(validateSettings({ ps_minimum: 50 }).ps_minimum).toBe(30);
  });
});

describe('Settings Validation - Booleans', () => {
  it('should convert truthy values to true', () => {
    const result = validateSettings({
      alerts_enabled: true,
      balance_alert_enabled: 1 as any,
      te_alert_enabled: 'yes' as any,
    });

    expect(result.alerts_enabled).toBe(true);
    expect(result.balance_alert_enabled).toBe(true);
    expect(result.te_alert_enabled).toBe(true);
  });

  it('should convert falsy values to false', () => {
    const result = validateSettings({
      alerts_enabled: false,
      balance_alert_enabled: 0 as any,
      te_alert_enabled: '' as any,
    });

    expect(result.alerts_enabled).toBe(false);
    expect(result.balance_alert_enabled).toBe(false);
    expect(result.te_alert_enabled).toBe(false);
  });

  it('should not include undefined booleans', () => {
    const result = validateSettings({ balance_threshold: 5 });
    expect(result.alerts_enabled).toBeUndefined();
  });
});

describe('Settings Validation - Triggers', () => {
  it('should accept valid trigger values', () => {
    expect(validateSettings({ balance_alert_trigger: 'PROBLEM_ONLY' }).balance_alert_trigger)
      .toBe('PROBLEM_ONLY');
    expect(validateSettings({ te_alert_trigger: 'ATTENTION_AND_PROBLEM' }).te_alert_trigger)
      .toBe('ATTENTION_AND_PROBLEM');
  });

  it('should reject invalid trigger values', () => {
    const result = validateSettings({ balance_alert_trigger: 'INVALID' as any });
    expect(result.balance_alert_trigger).toBeUndefined();
  });

  it('should handle all three trigger settings', () => {
    const result = validateSettings({
      balance_alert_trigger: 'ATTENTION_AND_PROBLEM',
      te_alert_trigger: 'PROBLEM_ONLY',
      ps_alert_trigger: 'ATTENTION_AND_PROBLEM',
    });

    expect(result.balance_alert_trigger).toBe('ATTENTION_AND_PROBLEM');
    expect(result.te_alert_trigger).toBe('PROBLEM_ONLY');
    expect(result.ps_alert_trigger).toBe('ATTENTION_AND_PROBLEM');
  });
});

describe('Settings Validation - Cooldowns', () => {
  it('should clamp cooldowns to 10-120', () => {
    expect(validateSettings({ balance_alert_cooldown: 5 }).balance_alert_cooldown).toBe(10);
    expect(validateSettings({ te_alert_cooldown: 60 }).te_alert_cooldown).toBe(60);
    expect(validateSettings({ ps_alert_cooldown: 200 }).ps_alert_cooldown).toBe(120);
  });

  it('should handle all three cooldown settings', () => {
    const result = validateSettings({
      balance_alert_cooldown: 15,
      te_alert_cooldown: 45,
      ps_alert_cooldown: 90,
    });

    expect(result.balance_alert_cooldown).toBe(15);
    expect(result.te_alert_cooldown).toBe(45);
    expect(result.ps_alert_cooldown).toBe(90);
  });
});

describe('Settings Validation - Edge Cases', () => {
  it('should handle empty input', () => {
    const result = validateSettings({});
    expect(Object.keys(result)).toHaveLength(0);
  });

  it('should ignore unknown fields', () => {
    const result = validateSettings({ unknown_field: 'value' } as any);
    expect(result).not.toHaveProperty('unknown_field');
  });

  it('should handle multiple settings at once', () => {
    const result = validateSettings({
      balance_threshold: 3,
      alerts_enabled: false,
      balance_alert_trigger: 'ATTENTION_AND_PROBLEM',
      te_alert_cooldown: 60,
    });

    expect(result.balance_threshold).toBe(3);
    expect(result.alerts_enabled).toBe(false);
    expect(result.balance_alert_trigger).toBe('ATTENTION_AND_PROBLEM');
    expect(result.te_alert_cooldown).toBe(60);
  });

  it('should handle NaN input for numbers', () => {
    const result = validateSettings({ balance_threshold: NaN });
    // NaN propagates through Math.max/min - this is JS behavior
    // In practice, API validates input before this point
    expect(result.balance_threshold).toBeNaN();
  });
});

describe('User Isolation', () => {
  let db: MockSettingsDatabase;

  beforeEach(() => {
    db = new MockSettingsDatabase();
  });

  it('different users have independent settings', () => {
    handlePutSettings(db, 'user-1', { balance_threshold: 3 });
    handlePutSettings(db, 'user-2', { balance_threshold: 7 });

    const user1Settings = handleGetSettings(db, 'user-1').data.settings;
    const user2Settings = handleGetSettings(db, 'user-2').data.settings;

    expect(user1Settings.balance_threshold).toBe(3);
    expect(user2Settings.balance_threshold).toBe(7);
  });

  it('updating one user does not affect another', () => {
    db.saveSettings('user-1', { ...DEFAULT_SETTINGS, alerts_enabled: true });
    db.saveSettings('user-2', { ...DEFAULT_SETTINGS, alerts_enabled: true });

    handlePutSettings(db, 'user-1', { alerts_enabled: false });

    expect(handleGetSettings(db, 'user-1').data.settings.alerts_enabled).toBe(false);
    expect(handleGetSettings(db, 'user-2').data.settings.alerts_enabled).toBe(true);
  });
});

describe('Default Settings', () => {
  it('should have sensible defaults', () => {
    expect(DEFAULT_SETTINGS.balance_threshold).toBe(5);
    expect(DEFAULT_SETTINGS.te_optimal_min).toBe(70);
    expect(DEFAULT_SETTINGS.te_optimal_max).toBe(80);
    expect(DEFAULT_SETTINGS.ps_minimum).toBe(20);
  });

  it('should have alerts enabled by default', () => {
    expect(DEFAULT_SETTINGS.alerts_enabled).toBe(true);
    expect(DEFAULT_SETTINGS.balance_alert_enabled).toBe(true);
    expect(DEFAULT_SETTINGS.te_alert_enabled).toBe(true);
    expect(DEFAULT_SETTINGS.ps_alert_enabled).toBe(true);
  });

  it('should have sound disabled by default (less intrusive)', () => {
    expect(DEFAULT_SETTINGS.balance_alert_sound).toBe(false);
    expect(DEFAULT_SETTINGS.te_alert_sound).toBe(false);
    expect(DEFAULT_SETTINGS.ps_alert_sound).toBe(false);
  });

  it('should have vibration enabled by default', () => {
    expect(DEFAULT_SETTINGS.balance_alert_vibration).toBe(true);
    expect(DEFAULT_SETTINGS.te_alert_vibration).toBe(true);
    expect(DEFAULT_SETTINGS.ps_alert_vibration).toBe(true);
  });

  it('should use PROBLEM_ONLY trigger by default', () => {
    expect(DEFAULT_SETTINGS.balance_alert_trigger).toBe('PROBLEM_ONLY');
    expect(DEFAULT_SETTINGS.te_alert_trigger).toBe('PROBLEM_ONLY');
    expect(DEFAULT_SETTINGS.ps_alert_trigger).toBe('PROBLEM_ONLY');
  });

  it('should have 30 second cooldowns by default', () => {
    expect(DEFAULT_SETTINGS.balance_alert_cooldown).toBe(30);
    expect(DEFAULT_SETTINGS.te_alert_cooldown).toBe(30);
    expect(DEFAULT_SETTINGS.ps_alert_cooldown).toBe(30);
  });

  it('should have sync enabled by default', () => {
    expect(DEFAULT_SETTINGS.background_mode_enabled).toBe(true);
    expect(DEFAULT_SETTINGS.auto_sync_enabled).toBe(true);
  });
});
