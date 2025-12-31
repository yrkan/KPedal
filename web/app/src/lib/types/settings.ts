export interface Device {
  id: string;
  name: string;
  type: string;
  status: 'connected' | 'idle' | 'offline';
  last_sync: string | null;
  last_sync_relative: string | null;
  created_at: string;
}

export type AlertTrigger = 'PROBLEM_ONLY' | 'ATTENTION_AND_PROBLEM';

export interface KPedalSettings {
  balance_threshold: number;
  te_optimal_min: number;
  te_optimal_max: number;
  ps_minimum: number;
  alerts_enabled: boolean;
  screen_wake_on_alert: boolean;
  balance_alert_enabled: boolean;
  balance_alert_trigger: AlertTrigger;
  balance_alert_visual: boolean;
  balance_alert_vibration: boolean;
  balance_alert_sound: boolean;
  balance_alert_cooldown: number;
  te_alert_enabled: boolean;
  te_alert_trigger: AlertTrigger;
  te_alert_visual: boolean;
  te_alert_vibration: boolean;
  te_alert_sound: boolean;
  te_alert_cooldown: number;
  ps_alert_enabled: boolean;
  ps_alert_trigger: AlertTrigger;
  ps_alert_visual: boolean;
  ps_alert_vibration: boolean;
  ps_alert_sound: boolean;
  ps_alert_cooldown: number;
  background_mode_enabled: boolean;
  auto_sync_enabled: boolean;
  updated_at?: string;
}

export const DEFAULT_SETTINGS: KPedalSettings = {
  balance_threshold: 5,
  te_optimal_min: 70,
  te_optimal_max: 80,
  ps_minimum: 20,
  alerts_enabled: true,
  screen_wake_on_alert: true,
  balance_alert_enabled: true,
  balance_alert_trigger: 'PROBLEM_ONLY',
  balance_alert_visual: true,
  balance_alert_vibration: true,
  balance_alert_sound: false,
  balance_alert_cooldown: 30,
  te_alert_enabled: true,
  te_alert_trigger: 'PROBLEM_ONLY',
  te_alert_visual: true,
  te_alert_vibration: true,
  te_alert_sound: false,
  te_alert_cooldown: 30,
  ps_alert_enabled: true,
  ps_alert_trigger: 'PROBLEM_ONLY',
  ps_alert_visual: true,
  ps_alert_vibration: true,
  ps_alert_sound: false,
  ps_alert_cooldown: 30,
  background_mode_enabled: true,
  auto_sync_enabled: true,
};
