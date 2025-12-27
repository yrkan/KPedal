<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { auth, isAuthenticated, user, authFetch } from '$lib/auth';
  import { theme, type Theme } from '$lib/theme';
  import { t, locale, setLocale, locales, localeNames, type Locale } from '$lib/i18n';
  import InfoTip from '$lib/components/InfoTip.svelte';

  interface Device {
    id: string;
    name: string;
    type: string;
    status: 'connected' | 'idle' | 'offline';
    last_sync: string | null;
    last_sync_relative: string | null;
    created_at: string;
  }

  type AlertTrigger = 'PROBLEM_ONLY' | 'ATTENTION_AND_PROBLEM';

  interface KPedalSettings {
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

  const DEFAULT_SETTINGS: KPedalSettings = {
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

  let devices: Device[] = [];
  let devicesLoading = true;
  let devicesError = false;
  let removingDevice: string | null = null;
  let requestingSyncDevice: string | null = null;
  let syncRequestedDevice: string | null = null;

  // KPedal settings state
  let kpedalSettings: KPedalSettings = { ...DEFAULT_SETTINGS };
  let settingsLoading = true;
  let settingsError = false;
  let savingSettings = false;
  let settingsSaved = false;
  let saveTimeout: ReturnType<typeof setTimeout> | null = null;

  onMount(async () => {
    if (!$isAuthenticated) {
      goto('/login');
      return;
    }
    await Promise.all([loadDevices(), loadSettings()]);
  });

  async function loadDevices() {
    devicesLoading = true;
    devicesError = false;
    try {
      const res = await authFetch('/devices');
      const data = await res.json();
      if (data.success) {
        devices = data.data.devices;
      } else {
        devicesError = true;
      }
    } catch (err) {
      devicesError = true;
    } finally {
      devicesLoading = false;
    }
  }

  async function removeDevice(deviceId: string) {
    if (!confirm($t('settings.removeDevice'))) return;

    removingDevice = deviceId;
    try {
      const res = await authFetch(`/devices/${deviceId}`, { method: 'DELETE' });
      const data = await res.json();
      if (data.success) {
        devices = devices.filter(d => d.id !== deviceId);
      }
    } catch (err) {
      // ignore
    } finally {
      removingDevice = null;
    }
  }

  async function requestSync(deviceId: string) {
    requestingSyncDevice = deviceId;
    try {
      const res = await authFetch(`/devices/${deviceId}/request-sync`, { method: 'POST' });
      const data = await res.json();
      if (data.success) {
        syncRequestedDevice = deviceId;
        // Clear the success message after 5 seconds
        setTimeout(() => {
          if (syncRequestedDevice === deviceId) {
            syncRequestedDevice = null;
          }
        }, 5000);
      }
    } catch (err) {
      // ignore
    } finally {
      requestingSyncDevice = null;
    }
  }

  function setTheme(newTheme: Theme) {
    theme.set(newTheme);
  }

  function getStatusColor(status: string): string {
    switch (status) {
      case 'connected': return 'var(--color-optimal)';
      case 'idle': return 'var(--color-attention)';
      default: return 'var(--text-muted)';
    }
  }

  function getStatusLabel(status: string): string {
    switch (status) {
      case 'connected': return $t('settings.deviceStatus.connected');
      case 'idle': return $t('settings.deviceStatus.idle');
      default: return $t('settings.deviceStatus.offline');
    }
  }

  // Load KPedal settings from server
  async function loadSettings() {
    settingsLoading = true;
    settingsError = false;
    try {
      const res = await authFetch('/settings');
      const data = await res.json();
      if (data.success && data.data?.settings) {
        kpedalSettings = { ...DEFAULT_SETTINGS, ...data.data.settings };
      }
    } catch (err) {
      settingsError = true;
    } finally {
      settingsLoading = false;
    }
  }

  // Save settings with debounce
  async function saveSettings(changes: Partial<KPedalSettings>) {
    // Apply changes immediately for responsive UI
    kpedalSettings = { ...kpedalSettings, ...changes };

    // Clear previous timeout
    if (saveTimeout) clearTimeout(saveTimeout);

    // Debounce API call
    saveTimeout = setTimeout(async () => {
      savingSettings = true;
      settingsSaved = false;
      try {
        const res = await authFetch('/settings', {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(changes),
        });
        const data = await res.json();
        if (data.success && data.data?.settings) {
          kpedalSettings = { ...DEFAULT_SETTINGS, ...data.data.settings };
          settingsSaved = true;
          setTimeout(() => settingsSaved = false, 2000);
        }
      } catch (err) {
        console.error('Failed to save settings:', err);
      } finally {
        savingSettings = false;
      }
    }, 500);
  }
</script>

<svelte:head>
  <title>{$t('settings.title')} - KPedal</title>
</svelte:head>

<div class="page settings-page">
  <div class="container container-md">
    <header class="page-header animate-in">
      <h1>{$t('settings.title')}</h1>
    </header>

    <!-- Account -->
    <section class="settings-section animate-in">
      <div class="section-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
          <circle cx="12" cy="7" r="4"/>
        </svg>
        <h2>{$t('settings.account')}</h2>
      </div>
      <div class="settings-card">
        <div class="account-row">
          {#if $user?.picture}
            <img src={$user.picture} alt="" class="account-avatar" referrerpolicy="no-referrer" />
          {:else}
            <div class="account-avatar-placeholder">{$user?.name?.[0] || '?'}</div>
          {/if}
          <div class="account-info">
            <span class="account-name">{$user?.name}</span>
            <span class="account-email">{$user?.email}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Devices -->
    <section class="settings-section animate-in">
      <div class="section-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="4" y="2" width="16" height="20" rx="3"/>
          <circle cx="12" cy="17" r="1.5"/>
          <line x1="8" y1="6" x2="16" y2="6"/>
          <line x1="8" y1="9" x2="14" y2="9"/>
        </svg>
        <h2>{$t('settings.connectedDevices')}</h2>
      </div>
      <div class="settings-card">
        {#if devicesLoading}
          <div class="devices-loading">
            <div class="spinner-small"></div>
            <span>{$t('settings.loadingDevices')}</span>
          </div>
        {:else if devicesError}
          <div class="devices-error">
            <span>{$t('errors.failedToLoadDevices')}</span>
            <button class="text-btn" on:click={loadDevices}>{$t('common.retry')}</button>
          </div>
        {:else if devices.length === 0}
          <div class="devices-empty">
            <div class="empty-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="4" y="2" width="16" height="20" rx="3"/>
                <circle cx="12" cy="17" r="1.5"/>
                <line x1="8" y1="6" x2="16" y2="6"/>
                <line x1="8" y1="9" x2="14" y2="9"/>
              </svg>
            </div>
            <p class="empty-text">{$t('settings.noDevices')}</p>
            <p class="empty-hint">{$t('settings.noDevicesHint')}</p>
            <a href="/link" class="link-device-btn">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
              {$t('settings.linkDevice')}
            </a>
          </div>
        {:else}
          <div class="devices-list">
            {#each devices as device (device.id)}
              <div class="device-item">
                <div class="device-icon">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="4" y="2" width="16" height="20" rx="3"/>
                    <circle cx="12" cy="17" r="1.5"/>
                    <line x1="8" y1="6" x2="16" y2="6"/>
                    <line x1="8" y1="9" x2="14" y2="9"/>
                  </svg>
                </div>
                <div class="device-info">
                  <div class="device-name-row">
                    <span class="device-name">{device.name}</span>
                    <span class="device-status" style="--status-color: {getStatusColor(device.status)}">
                      <span class="status-dot"></span>
                      {getStatusLabel(device.status)}
                    </span>
                  </div>
                  <div class="device-meta">
                    {#if syncRequestedDevice === device.id}
                      <span class="sync-requested">{$t('settings.syncRequested')}</span>
                    {:else if device.last_sync_relative}
                      <span>{$t('settings.lastSync')}: {device.last_sync_relative}</span>
                    {:else}
                      <span>{$t('settings.neverSynced')}</span>
                    {/if}
                  </div>
                </div>
                <button
                  class="device-sync"
                  on:click={() => requestSync(device.id)}
                  disabled={requestingSyncDevice === device.id || syncRequestedDevice === device.id}
                  title={$t('settings.requestSync')}
                >
                  {#if requestingSyncDevice === device.id}
                    <div class="spinner-tiny"></div>
                  {:else if syncRequestedDevice === device.id}
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                  {:else}
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M23 4v6h-6"/>
                      <path d="M1 20v-6h6"/>
                      <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
                    </svg>
                  {/if}
                </button>
                <button
                  class="device-remove"
                  on:click={() => removeDevice(device.id)}
                  disabled={removingDevice === device.id}
                  title={$t('common.remove')}
                >
                  {#if removingDevice === device.id}
                    <div class="spinner-tiny"></div>
                  {:else}
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <line x1="18" y1="6" x2="6" y2="18"/>
                      <line x1="6" y1="6" x2="18" y2="18"/>
                    </svg>
                  {/if}
                </button>
              </div>
            {/each}
          </div>
          <div class="devices-footer">
            <a href="/link" class="add-device-link">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
              {$t('settings.linkAnotherDevice')}
            </a>
          </div>
        {/if}
      </div>
    </section>

    <!-- Pedaling Metrics Settings -->
    <section class="settings-section animate-in">
      <div class="section-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 20v-6M6 20V10M18 20V4"/>
        </svg>
        <h2>{$t('settings.pedalingMetrics')}</h2>
      </div>

      <!-- Sync Banner -->
      <div class="sync-info-banner">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 12a9 9 0 0 1-9 9m9-9a9 9 0 0 0-9-9m9 9H3m9 9a9 9 0 0 1-9-9m9 9c-1.657 0-3-4.03-3-9s1.343-9 3-9m0 18c1.657 0 3-4.03 3-9s-1.343-9-3-9"/>
        </svg>
        <span>{@html $t('settings.syncBanner')}</span>
      </div>

      {#if settingsLoading}
        <div class="settings-card">
          <div class="devices-loading">
            <div class="spinner-small"></div>
            <span>{$t('common.loading')}</span>
          </div>
        </div>
      {:else if settingsError}
        <div class="settings-card">
          <div class="devices-error">
            <span>{$t('errors.failedToLoadSettings')}</span>
            <button class="text-btn" on:click={loadSettings}>{$t('common.retry')}</button>
          </div>
        </div>
      {:else}
        <div class="metrics-settings">
          <!-- Thresholds Card -->
          <div class="metrics-card">
            <div class="metrics-card-header">
              <h3>{$t('settings.thresholds')}</h3>
            </div>
            <div class="metrics-card-body thresholds-body">
              <!-- Balance Threshold -->
              <div class="threshold-row">
                <div class="threshold-info">
                  <div class="threshold-label-row">
                    <span class="threshold-label">{$t('settings.balanceThreshold')}</span>
                    <InfoTip text={$t('settings.balanceThresholdTip')} position="right" size="sm" />
                  </div>
                  <span class="threshold-hint">{$t('settings.balanceThresholdHint')}</span>
                </div>
                <div class="threshold-input-group">
                  <button
                    class="threshold-btn"
                    disabled={kpedalSettings.balance_threshold <= 1}
                    on:click={() => saveSettings({ balance_threshold: Math.max(1, kpedalSettings.balance_threshold - 1) })}
                  >−</button>
                  <span class="threshold-value">±{kpedalSettings.balance_threshold}%</span>
                  <button
                    class="threshold-btn"
                    disabled={kpedalSettings.balance_threshold >= 10}
                    on:click={() => saveSettings({ balance_threshold: Math.min(10, kpedalSettings.balance_threshold + 1) })}
                  >+</button>
                </div>
              </div>

              <!-- TE Optimal Range -->
              <div class="threshold-row">
                <div class="threshold-info">
                  <div class="threshold-label-row">
                    <span class="threshold-label">{$t('settings.teOptimalRange')}</span>
                    <InfoTip text={$t('settings.teOptimalRangeTip')} position="right" size="sm" />
                  </div>
                  <span class="threshold-hint">{$t('settings.teOptimalRangeHint')}</span>
                </div>
                <div class="threshold-range-group">
                  <div class="threshold-input-group small">
                    <button
                      class="threshold-btn"
                      disabled={kpedalSettings.te_optimal_min <= 50}
                      on:click={() => saveSettings({ te_optimal_min: Math.max(50, kpedalSettings.te_optimal_min - 5) })}
                    >−</button>
                    <span class="threshold-value">{kpedalSettings.te_optimal_min}%</span>
                    <button
                      class="threshold-btn"
                      disabled={kpedalSettings.te_optimal_min >= kpedalSettings.te_optimal_max - 5}
                      on:click={() => saveSettings({ te_optimal_min: Math.min(kpedalSettings.te_optimal_max - 5, kpedalSettings.te_optimal_min + 5) })}
                    >+</button>
                  </div>
                  <span class="threshold-range-sep">–</span>
                  <div class="threshold-input-group small">
                    <button
                      class="threshold-btn"
                      disabled={kpedalSettings.te_optimal_max <= kpedalSettings.te_optimal_min + 5}
                      on:click={() => saveSettings({ te_optimal_max: Math.max(kpedalSettings.te_optimal_min + 5, kpedalSettings.te_optimal_max - 5) })}
                    >−</button>
                    <span class="threshold-value">{kpedalSettings.te_optimal_max}%</span>
                    <button
                      class="threshold-btn"
                      disabled={kpedalSettings.te_optimal_max >= 100}
                      on:click={() => saveSettings({ te_optimal_max: Math.min(100, kpedalSettings.te_optimal_max + 5) })}
                    >+</button>
                  </div>
                </div>
              </div>

              <!-- PS Minimum -->
              <div class="threshold-row">
                <div class="threshold-info">
                  <div class="threshold-label-row">
                    <span class="threshold-label">{$t('settings.psMinimum')}</span>
                    <InfoTip text={$t('settings.psMinimumTip')} position="right" size="sm" />
                  </div>
                  <span class="threshold-hint">{$t('settings.psMinimumHint')}</span>
                </div>
                <div class="threshold-input-group">
                  <button
                    class="threshold-btn"
                    disabled={kpedalSettings.ps_minimum <= 10}
                    on:click={() => saveSettings({ ps_minimum: Math.max(10, kpedalSettings.ps_minimum - 5) })}
                  >−</button>
                  <span class="threshold-value">{kpedalSettings.ps_minimum}%</span>
                  <button
                    class="threshold-btn"
                    disabled={kpedalSettings.ps_minimum >= 30}
                    on:click={() => saveSettings({ ps_minimum: Math.min(30, kpedalSettings.ps_minimum + 5) })}
                  >+</button>
                </div>
              </div>
            </div>
          </div>

          <!-- Alerts Card -->
          <div class="metrics-card">
            <div class="metrics-card-header">
              <div class="header-with-toggle">
                <div>
                  <h3>{$t('settings.inRideAlerts')} <InfoTip text={$t('settings.inRideAlertsTip')} position="right" size="sm" /></h3>
                  <span class="metrics-card-hint">{$t('settings.inRideAlertsHint')}</span>
                </div>
                <label class="toggle">
                  <input
                    type="checkbox"
                    checked={kpedalSettings.alerts_enabled}
                    on:change={(e) => saveSettings({ alerts_enabled: e.currentTarget.checked })}
                  />
                  <span class="toggle-slider"></span>
                </label>
              </div>
            </div>
            {#if kpedalSettings.alerts_enabled}
              <div class="metrics-card-body alerts-body">
                <!-- Metrics to Monitor -->
                <div class="alert-section">
                  <span class="alert-section-label">{$t('settings.metricsToMonitor')}</span>
                  <div class="alert-metrics-row">
                    <label class="alert-metric-check" class:active={kpedalSettings.balance_alert_enabled}>
                      <input type="checkbox" checked={kpedalSettings.balance_alert_enabled}
                        on:change={() => saveSettings({ balance_alert_enabled: !kpedalSettings.balance_alert_enabled })} />
                      <span class="metric-dot" style="background: var(--color-attention)"></span>
                      <span>{$t('metrics.balance')}</span>
                    </label>
                    <label class="alert-metric-check" class:active={kpedalSettings.te_alert_enabled}>
                      <input type="checkbox" checked={kpedalSettings.te_alert_enabled}
                        on:change={() => saveSettings({ te_alert_enabled: !kpedalSettings.te_alert_enabled })} />
                      <span class="metric-dot" style="background: var(--color-optimal)"></span>
                      <span>{$t('settings.torqueEff')}</span>
                    </label>
                    <label class="alert-metric-check" class:active={kpedalSettings.ps_alert_enabled}>
                      <input type="checkbox" checked={kpedalSettings.ps_alert_enabled}
                        on:change={() => saveSettings({ ps_alert_enabled: !kpedalSettings.ps_alert_enabled })} />
                      <span class="metric-dot" style="background: var(--color-problem)"></span>
                      <span>{$t('settings.smoothness')}</span>
                    </label>
                  </div>
                </div>

                <!-- Sensitivity -->
                <div class="alert-section">
                  <span class="alert-section-label">{$t('settings.sensitivity')}</span>
                  <div class="sensitivity-chips">
                    <button
                      class="sensitivity-chip"
                      class:active={kpedalSettings.balance_alert_trigger === 'PROBLEM_ONLY'}
                      on:click={() => saveSettings({
                        balance_alert_trigger: 'PROBLEM_ONLY',
                        te_alert_trigger: 'PROBLEM_ONLY',
                        ps_alert_trigger: 'PROBLEM_ONLY'
                      })}
                    >
                      <span class="sensitivity-dots">
                        <span class="dot-problem"></span>
                      </span>
                      <span>{$t('settings.critical')}</span>
                      <span class="sensitivity-hint">{$t('settings.problemZoneOnly')}</span>
                    </button>
                    <button
                      class="sensitivity-chip"
                      class:active={kpedalSettings.balance_alert_trigger === 'ATTENTION_AND_PROBLEM'}
                      on:click={() => saveSettings({
                        balance_alert_trigger: 'ATTENTION_AND_PROBLEM',
                        te_alert_trigger: 'ATTENTION_AND_PROBLEM',
                        ps_alert_trigger: 'ATTENTION_AND_PROBLEM'
                      })}
                    >
                      <span class="sensitivity-dots">
                        <span class="dot-attention"></span>
                        <span class="dot-problem"></span>
                      </span>
                      <span>{$t('settings.sensitive')}</span>
                      <span class="sensitivity-hint">{$t('settings.attentionAndProblem')}</span>
                    </button>
                  </div>
                </div>

                <!-- Notify Via -->
                <div class="alert-section">
                  <span class="alert-section-label">{$t('settings.notifyVia')}</span>
                  <div class="notify-chips">
                    <button
                      class="notify-chip"
                      class:active={kpedalSettings.balance_alert_vibration}
                      on:click={() => saveSettings({
                        balance_alert_vibration: !kpedalSettings.balance_alert_vibration,
                        te_alert_vibration: !kpedalSettings.balance_alert_vibration,
                        ps_alert_vibration: !kpedalSettings.balance_alert_vibration
                      })}
                    >
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                      </svg>
                      {$t('settings.vibrate')}
                    </button>
                    <button
                      class="notify-chip"
                      class:active={kpedalSettings.balance_alert_sound}
                      on:click={() => saveSettings({
                        balance_alert_sound: !kpedalSettings.balance_alert_sound,
                        te_alert_sound: !kpedalSettings.balance_alert_sound,
                        ps_alert_sound: !kpedalSettings.balance_alert_sound
                      })}
                    >
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/>
                        <path d="M15.54 8.46a5 5 0 0 1 0 7.07"/>
                        <path d="M19.07 4.93a10 10 0 0 1 0 14.14"/>
                      </svg>
                      {$t('settings.sound')}
                    </button>
                    <button
                      class="notify-chip"
                      class:active={kpedalSettings.balance_alert_visual}
                      on:click={() => saveSettings({
                        balance_alert_visual: !kpedalSettings.balance_alert_visual,
                        te_alert_visual: !kpedalSettings.balance_alert_visual,
                        ps_alert_visual: !kpedalSettings.balance_alert_visual
                      })}
                    >
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <rect x="3" y="3" width="18" height="18" rx="2"/>
                        <path d="M3 9h18"/>
                      </svg>
                      {$t('settings.banner')}
                    </button>
                  </div>
                </div>

                <!-- Options -->
                <div class="alert-section">
                  <span class="alert-section-label">{$t('settings.options')}</span>
                  <div class="alert-options">
                    <div class="alert-option-row">
                      <span class="alert-option-label">{$t('settings.wakeScreenOnAlert')}</span>
                      <label class="toggle toggle-sm">
                        <input type="checkbox" checked={kpedalSettings.screen_wake_on_alert}
                          on:change={(e) => saveSettings({ screen_wake_on_alert: e.currentTarget.checked })} />
                        <span class="toggle-slider"></span>
                      </label>
                    </div>
                    <div class="alert-option-row">
                      <span class="alert-option-label">{$t('settings.cooldownBetweenAlerts')}</span>
                      <div class="cooldown-chips">
                        {#each [{ value: 15, label: '15s' }, { value: 30, label: '30s' }, { value: 60, label: '1m' }, { value: 120, label: '2m' }] as opt}
                          <button
                            class="cooldown-chip"
                            class:active={kpedalSettings.balance_alert_cooldown === opt.value}
                            on:click={() => saveSettings({
                              balance_alert_cooldown: opt.value,
                              te_alert_cooldown: opt.value,
                              ps_alert_cooldown: opt.value
                            })}
                          >{opt.label}</button>
                        {/each}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            {/if}
          </div>

          <!-- Data Collection Card -->
          <div class="metrics-card">
            <div class="metrics-card-header">
              <h3>{$t('settings.dataCollection')}</h3>
              <span class="metrics-card-hint">{$t('settings.dataCollectionHint')}</span>
            </div>
            <div class="metrics-card-body">
              <div class="data-option">
                <div class="data-option-info">
                  <span class="data-option-name">{$t('settings.backgroundMode')} <InfoTip text={$t('settings.backgroundModeTip')} position="right" size="sm" /></span>
                  <span class="data-option-desc">{$t('settings.backgroundModeHint')}</span>
                </div>
                <label class="toggle">
                  <input
                    type="checkbox"
                    checked={kpedalSettings.background_mode_enabled}
                    on:change={(e) => saveSettings({ background_mode_enabled: e.currentTarget.checked })}
                  />
                  <span class="toggle-slider"></span>
                </label>
              </div>
              <div class="data-option">
                <div class="data-option-info">
                  <span class="data-option-name">{$t('settings.autoSync')} <InfoTip text={$t('settings.autoSyncTip')} position="right" size="sm" /></span>
                  <span class="data-option-desc">{$t('settings.autoSyncHint')}</span>
                </div>
                <label class="toggle">
                  <input
                    type="checkbox"
                    checked={kpedalSettings.auto_sync_enabled}
                    on:change={(e) => saveSettings({ auto_sync_enabled: e.currentTarget.checked })}
                  />
                  <span class="toggle-slider"></span>
                </label>
              </div>
            </div>
          </div>
        </div>
      {/if}
    </section>

    <!-- Appearance -->
    <section class="settings-section animate-in">
      <div class="section-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="3"/>
          <path d="M12 1v2m0 18v2M4.22 4.22l1.42 1.42m12.72 12.72 1.42 1.42M1 12h2m18 0h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
        </svg>
        <h2>{$t('settings.appearance')}</h2>
      </div>
      <div class="settings-card">
        <div class="setting-row">
          <div class="setting-info">
            <span class="setting-label">{$t('settings.theme')}</span>
            <span class="setting-description">{$t('settings.themeHint')}</span>
          </div>
          <div class="theme-selector">
            <button
              class="theme-option"
              class:active={$theme === 'auto'}
              on:click={() => setTheme('auto')}
              aria-label={$t('settings.themeAuto')}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/><polyline points="12,6 12,12 16,14"/>
              </svg>
              {$t('settings.themeAuto')}
            </button>
            <button
              class="theme-option"
              class:active={$theme === 'light'}
              on:click={() => setTheme('light')}
              aria-label={$t('settings.themeLight')}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="5"/>
                <line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
                <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
                <line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>
                <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
              </svg>
              {$t('settings.themeLight')}
            </button>
            <button
              class="theme-option"
              class:active={$theme === 'dark'}
              on:click={() => setTheme('dark')}
              aria-label={$t('settings.themeDark')}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
              </svg>
              {$t('settings.themeDark')}
            </button>
          </div>
        </div>
        <div class="setting-row">
          <div class="setting-info">
            <span class="setting-label">{$t('settings.language.title')}</span>
            <span class="setting-description">{$t('settings.language.description')}</span>
          </div>
          <div class="language-selector">
            {#each locales as loc}
              <button
                class="language-option"
                class:active={$locale === loc}
                on:click={() => setLocale(loc)}
                aria-label={localeNames[loc]}
              >
                <span class="lang-flag">{loc === 'en' ? '🇺🇸' : '🇪🇸'}</span>
                <span class="lang-name">{localeNames[loc]}</span>
              </button>
            {/each}
          </div>
        </div>
      </div>
    </section>

    <!-- Data -->
    <section class="settings-section animate-in">
      <div class="section-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21.21 15.89A10 10 0 1 1 8 2.83"/>
          <path d="M22 12A10 10 0 0 0 12 2v10z"/>
        </svg>
        <h2>{$t('settings.data')}</h2>
      </div>
      <div class="settings-card">
        <div class="setting-row">
          <div class="setting-info">
            <span class="setting-label">{$t('settings.rideData')}</span>
            <span class="setting-description">{$t('settings.rideDataHint')}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Session -->
    <section class="settings-section animate-in">
      <div class="section-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
        </svg>
        <h2>{$t('settings.session')}</h2>
      </div>
      <div class="settings-card">
        <div class="setting-row">
          <div class="setting-info">
            <span class="setting-label">{$t('auth.signedInViaGoogle')}</span>
            <span class="setting-description">{$t('auth.sessionExpires')}</span>
          </div>
          <button class="btn btn-danger btn-sm" on:click={() => auth.logout()}>
            {$t('auth.signOut')}
          </button>
        </div>
      </div>
    </section>

    <!-- Privacy -->
    <section class="settings-section animate-in">
      <div class="section-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
          <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
        </svg>
        <h2>{$t('settings.privacy')}</h2>
      </div>
      <div class="settings-card">
        <a href="/privacy" class="setting-row setting-link">
          <div class="setting-info">
            <span class="setting-label">{$t('auth.privacyPolicy')}</span>
            <span class="setting-description">{$t('settings.privacyHint')}</span>
          </div>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9,6 15,12 9,18"/>
          </svg>
        </a>
      </div>
    </section>

    <!-- About -->
    <section class="settings-section animate-in">
      <div class="section-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/>
          <path d="M12 16v-4"/>
          <circle cx="12" cy="8" r="0.5" fill="currentColor"/>
        </svg>
        <h2>{$t('settings.about')}</h2>
      </div>
      <div class="settings-card">
        <div class="about-content">
          <p class="app-name">KPedal Web</p>
          <p class="app-version">Version 1.0.0</p>
          <p class="app-description">{$t('app.tagline')}</p>
        </div>
      </div>
    </section>
  </div>

  <!-- Save Toast -->
  {#if savingSettings || settingsSaved}
    <div class="save-toast" class:saving={savingSettings} class:saved={settingsSaved}>
      {#if savingSettings}
        <div class="toast-spinner"></div>
        <span>{$t('settings.saving')}</span>
      {:else}
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="20 6 9 17 4 12"/>
        </svg>
        <span>{$t('settings.saved')}</span>
      {/if}
    </div>
  {/if}
</div>

<style>
  /* Page-specific styles */
  .settings-page .page-header h1 {
    font-size: 28px;
    letter-spacing: -0.5px;
  }

  .settings-section {
    margin-bottom: 32px;
  }

  .section-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
  }

  .section-header svg {
    color: var(--text-muted);
  }

  .settings-section h2 {
    font-size: 12px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: var(--text-tertiary);
  }

  .settings-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 16px;
    padding: 20px;
  }

  .account-row {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .account-avatar {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    object-fit: cover;
  }

  .account-avatar-placeholder {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: var(--bg-elevated);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    font-weight: 600;
    color: var(--text-secondary);
  }

  .account-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .account-name {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
  }

  .account-email {
    font-size: 14px;
    color: var(--text-secondary);
  }

  .setting-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
  }

  .setting-link {
    text-decoration: none;
    transition: background 0.15s ease;
    margin: -20px;
    padding: 20px;
    border-radius: 16px;
  }

  .setting-link:hover {
    background: var(--bg-hover);
  }

  .setting-link svg {
    color: var(--text-muted);
    transition: color 0.15s ease;
  }

  .setting-link:hover svg {
    color: var(--text-secondary);
  }

  .setting-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .setting-label {
    font-size: 15px;
    font-weight: 500;
    color: var(--text-primary);
  }

  .setting-description {
    font-size: 13px;
    color: var(--text-tertiary);
  }

  .theme-selector {
    display: flex;
    gap: 8px;
  }

  .theme-option {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 12px;
    border-radius: 8px;
    background: var(--bg-elevated);
    border: 1px solid transparent;
    color: var(--text-secondary);
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s ease;
  }

  .theme-option:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
  }

  .theme-option.active {
    background: var(--color-accent-soft);
    color: var(--color-accent);
    border-color: var(--color-accent);
  }

  .language-selector {
    display: flex;
    gap: 8px;
  }

  .language-option {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 12px;
    border-radius: 8px;
    background: var(--bg-elevated);
    border: 1px solid transparent;
    color: var(--text-secondary);
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s ease;
  }

  .language-option:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
  }

  .language-option.active {
    background: var(--color-accent-soft);
    color: var(--color-accent);
    border-color: var(--color-accent);
  }

  .lang-flag {
    font-size: 16px;
  }

  .lang-name {
    font-weight: 500;
  }

  .about-content {
    text-align: center;
    padding: 12px 0;
  }

  .app-name {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 4px;
  }

  .app-version {
    font-size: 13px;
    color: var(--text-tertiary);
    margin-bottom: 8px;
  }

  .app-description {
    font-size: 13px;
    color: var(--text-muted);
  }

  /* Devices Section */
  .devices-loading,
  .devices-error {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    padding: 24px;
    color: var(--text-secondary);
    font-size: 14px;
  }

  .devices-error {
    flex-direction: column;
    gap: 8px;
  }

  .text-btn {
    background: none;
    border: none;
    color: var(--color-accent);
    font-size: 13px;
    cursor: pointer;
    padding: 4px 8px;
  }

  .text-btn:hover {
    text-decoration: underline;
  }

  .devices-empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 32px 16px;
    text-align: center;
  }

  .empty-icon {
    width: 56px;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-elevated);
    border-radius: 16px;
    color: var(--text-muted);
    margin-bottom: 16px;
  }

  .empty-text {
    font-size: 15px;
    font-weight: 500;
    color: var(--text-primary);
    margin: 0 0 4px 0;
  }

  .empty-hint {
    font-size: 13px;
    color: var(--text-tertiary);
    margin: 0 0 20px 0;
  }

  .link-device-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 10px 20px;
    background: var(--color-accent);
    color: var(--color-accent-text);
    border-radius: 10px;
    font-size: 14px;
    font-weight: 500;
    text-decoration: none;
    transition: all 0.15s ease;
  }

  .link-device-btn:hover {
    background: var(--color-accent-hover);
    transform: translateY(-1px);
    box-shadow: var(--shadow-md), var(--glow-accent);
  }

  .devices-list {
    display: flex;
    flex-direction: column;
  }

  .device-item {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 0;
    border-bottom: 1px solid var(--border-subtle);
  }

  .device-item:last-child {
    border-bottom: none;
  }

  .device-icon {
    width: 44px;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-elevated);
    border-radius: 12px;
    color: var(--text-secondary);
    flex-shrink: 0;
  }

  .device-info {
    flex: 1;
    min-width: 0;
  }

  .device-name-row {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 4px;
  }

  .device-name {
    font-size: 15px;
    font-weight: 500;
    color: var(--text-primary);
  }

  .device-status {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 11px;
    font-weight: 500;
    color: var(--status-color);
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }

  .status-dot {
    width: 6px;
    height: 6px;
    background: var(--status-color);
    border-radius: 50%;
  }

  .device-meta {
    font-size: 13px;
    color: var(--text-tertiary);
  }

  .sync-requested {
    color: var(--color-optimal-text);
  }

  .device-sync {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: transparent;
    border: none;
    border-radius: 8px;
    color: var(--text-muted);
    cursor: pointer;
    transition: all 0.15s ease;
    flex-shrink: 0;
  }

  .device-sync:hover:not(:disabled) {
    background: var(--color-accent-soft);
    color: var(--color-accent);
  }

  .device-sync:disabled {
    opacity: 0.7;
    cursor: not-allowed;
    color: var(--color-optimal-text);
  }

  .device-remove {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: transparent;
    border: none;
    border-radius: 8px;
    color: var(--text-muted);
    cursor: pointer;
    transition: all 0.15s ease;
    flex-shrink: 0;
  }

  .device-remove:hover:not(:disabled) {
    background: var(--color-problem-soft);
    color: var(--color-problem);
  }

  .device-remove:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .devices-footer {
    padding-top: 16px;
    border-top: 1px solid var(--border-subtle);
    margin-top: 8px;
  }

  .add-device-link {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: var(--color-accent);
    text-decoration: none;
  }

  .add-device-link:hover {
    text-decoration: underline;
  }

  .spinner-small {
    width: 18px;
    height: 18px;
    border: 2px solid var(--border-default);
    border-top-color: var(--color-accent);
    border-radius: 50%;
    animation: spin 0.7s linear infinite;
  }

  .spinner-tiny {
    width: 14px;
    height: 14px;
    border: 2px solid var(--border-default);
    border-top-color: var(--text-muted);
    border-radius: 50%;
    animation: spin 0.7s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  /* ============================================
     Pedaling Metrics Settings
     ============================================ */

  /* Save Toast - Fixed at bottom right */
  .save-toast {
    position: fixed;
    bottom: 100px;
    right: 24px;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 16px;
    background: var(--bg-elevated);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-secondary);
    box-shadow: var(--shadow-lg);
    z-index: 1000;
    animation: toast-in 0.2s ease-out;
  }

  .save-toast.saved {
    background: var(--color-optimal-soft);
    border-color: var(--color-optimal);
    color: var(--color-optimal-text);
  }

  .save-toast.saved svg {
    color: var(--color-optimal);
  }

  .toast-spinner {
    width: 14px;
    height: 14px;
    border: 2px solid var(--border-default);
    border-top-color: var(--text-secondary);
    border-radius: 50%;
    animation: spin 0.6s linear infinite;
  }

  @keyframes toast-in {
    from {
      opacity: 0;
      transform: translateY(10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  /* Mobile: position above bottom nav */
  @media (max-width: 768px) {
    .save-toast {
      bottom: 80px;
      right: 16px;
    }
  }

  .sync-info-banner {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 16px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
    margin-bottom: 16px;
    font-size: 13px;
    color: var(--text-secondary);
  }
  .sync-info-banner svg {
    flex-shrink: 0;
    color: var(--text-muted);
  }
  .sync-info-banner strong {
    color: var(--text-primary);
    font-weight: 600;
  }

  .metrics-settings {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .metrics-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 16px;
    overflow: hidden;
  }

  .metrics-card-header {
    padding: 16px 20px;
    border-bottom: 1px solid var(--border-subtle);
    background: var(--gradient-surface);
  }

  .metrics-card-header h3 {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 2px 0;
  }

  .metrics-card-hint {
    font-size: 12px;
    color: var(--text-tertiary);
  }

  .header-with-toggle {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .metrics-card-body {
    padding: 16px 20px;
  }

  /* Thresholds - Clean Stepper Design */
  .thresholds-body {
    display: flex;
    flex-direction: column;
    gap: 0;
  }

  .threshold-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 0;
    border-bottom: 1px solid var(--border-subtle);
    gap: 16px;
  }

  .threshold-row:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }

  .threshold-row:first-child {
    padding-top: 0;
  }

  .threshold-info {
    flex: 1;
    min-width: 0;
  }

  .threshold-label-row {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 2px;
  }

  .threshold-label {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-primary);
  }

  .threshold-hint {
    font-size: 12px;
    color: var(--text-tertiary);
    display: block;
  }

  .threshold-input-group {
    display: flex;
    align-items: center;
    gap: 2px;
    background: var(--bg-elevated);
    border: 1px solid var(--border-subtle);
    border-radius: 8px;
    padding: 2px;
    flex-shrink: 0;
  }

  .threshold-input-group.small {
    transform: scale(0.9);
    transform-origin: center;
  }

  .threshold-btn {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: transparent;
    border: none;
    border-radius: 6px;
    color: var(--text-primary);
    font-size: 18px;
    font-weight: 400;
    cursor: pointer;
    transition: background-color 0.15s;
    user-select: none;
    -webkit-tap-highlight-color: transparent;
  }

  .threshold-btn:hover:not(:disabled) {
    background: var(--bg-hover);
  }

  .threshold-btn:active:not(:disabled) {
    background: var(--border-subtle);
  }

  .threshold-btn:disabled {
    opacity: 0.3;
    cursor: not-allowed;
  }

  .threshold-value {
    min-width: 56px;
    text-align: center;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    font-variant-numeric: tabular-nums;
    padding: 0 4px;
  }

  .threshold-range-group {
    display: flex;
    align-items: center;
    gap: 4px;
    flex-shrink: 0;
  }

  .threshold-range-sep {
    font-size: 14px;
    color: var(--text-tertiary);
    padding: 0 2px;
  }

  /* Mobile adjustments */
  @media (max-width: 540px) {
    .threshold-row {
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
    }

    .threshold-input-group {
      align-self: flex-end;
    }

    .threshold-range-group {
      align-self: flex-end;
    }

    .threshold-input-group.small {
      transform: none;
    }

    .threshold-hint {
      display: none;
    }
  }


  /* In-Ride Alerts Styles */
  .alerts-body {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .alert-section {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .alert-section-label {
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: var(--text-tertiary);
  }

  /* Metrics to Monitor */
  .alert-metrics-row {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  .alert-metric-check {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 14px;
    background: var(--bg-elevated);
    border: 1px solid var(--border-subtle);
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.15s;
    font-size: 13px;
    color: var(--text-secondary);
  }

  .alert-metric-check input[type="checkbox"] {
    display: none;
  }

  .alert-metric-check:hover {
    border-color: var(--border-default);
  }

  .alert-metric-check.active {
    background: var(--bg-surface);
    border-color: var(--color-accent);
    color: var(--text-primary);
    box-shadow: inset 0 0 0 1px var(--color-accent);
  }

  .metric-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  /* Sensitivity Chips */
  .sensitivity-chips {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }

  .sensitivity-chip {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    padding: 14px 12px;
    background: var(--bg-elevated);
    border: 2px solid var(--border-subtle);
    border-radius: 10px;
    cursor: pointer;
    transition: all 0.15s;
  }

  .sensitivity-chip:hover {
    border-color: var(--border-default);
  }

  .sensitivity-chip.active {
    background: var(--bg-surface);
    border-color: var(--color-accent);
    box-shadow: inset 0 0 0 1px var(--color-accent);
  }

  .sensitivity-dots {
    display: flex;
    gap: 4px;
  }

  .dot-problem {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: var(--color-problem);
  }

  .dot-attention {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: var(--color-attention);
  }

  .sensitivity-chip span:not(.sensitivity-dots):not(.sensitivity-hint) {
    font-size: 13px;
    font-weight: 500;
    color: var(--text-primary);
  }

  .sensitivity-hint {
    font-size: 11px;
    color: var(--text-tertiary);
  }

  .sensitivity-chip.active .sensitivity-hint {
    color: var(--text-secondary);
  }

  /* Notify Via Chips */
  .notify-chips {
    display: flex;
    gap: 8px;
  }

  .notify-chip {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 12px 10px;
    background: var(--bg-elevated);
    border: 1px solid var(--border-subtle);
    border-radius: 8px;
    font-size: 12px;
    font-weight: 500;
    color: var(--text-secondary);
    cursor: pointer;
    transition: all 0.15s;
  }

  .notify-chip:hover {
    border-color: var(--border-default);
    color: var(--text-primary);
  }

  .notify-chip.active {
    background: var(--color-accent);
    border-color: var(--color-accent);
    color: var(--color-accent-text);
  }

  .notify-chip svg {
    flex-shrink: 0;
  }

  /* Options */
  .alert-options {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 12px 14px;
    background: var(--bg-elevated);
    border-radius: 10px;
  }

  .alert-option-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
  }

  .alert-option-label {
    font-size: 13px;
    color: var(--text-primary);
  }

  .toggle-sm {
    width: 38px;
    height: 20px;
  }

  .toggle-sm .toggle-slider:before {
    width: 14px;
    height: 14px;
  }

  .toggle-sm input:checked + .toggle-slider:before {
    transform: translateX(18px);
  }

  /* Cooldown Chips */
  .cooldown-chips {
    display: flex;
    gap: 4px;
  }

  .cooldown-chip {
    padding: 6px 10px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 6px;
    font-size: 11px;
    font-weight: 500;
    color: var(--text-tertiary);
    cursor: pointer;
    transition: all 0.15s;
  }

  .cooldown-chip:hover {
    border-color: var(--border-default);
    color: var(--text-secondary);
  }

  .cooldown-chip.active {
    background: var(--color-accent);
    border-color: var(--color-accent);
    color: var(--color-accent-text);
  }

  /* Data Options */
  .data-option {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid var(--border-subtle);
  }

  .data-option:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }

  .data-option:first-child {
    padding-top: 0;
  }

  .data-option-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .data-option-name {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-primary);
  }

  .data-option-desc {
    font-size: 12px;
    color: var(--text-tertiary);
  }

  /* Toggle Switch */
  .toggle {
    position: relative;
    display: inline-block;
    width: 44px;
    height: 24px;
    flex-shrink: 0;
  }

  .toggle input {
    opacity: 0;
    width: 0;
    height: 0;
  }

  .toggle-slider {
    position: absolute;
    cursor: pointer;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: var(--bg-elevated);
    border: 1px solid var(--border-default);
    border-radius: 24px;
    transition: 0.2s;
  }

  .toggle-slider:before {
    position: absolute;
    content: "";
    height: 18px;
    width: 18px;
    left: 2px;
    bottom: 2px;
    background-color: var(--text-tertiary);
    border-radius: 50%;
    transition: 0.2s;
  }

  .toggle input:checked + .toggle-slider {
    background-color: var(--color-accent);
    border-color: var(--color-accent);
  }

  .toggle input:checked + .toggle-slider:before {
    transform: translateX(20px);
    background-color: var(--color-accent-text);
  }

  /* ============================================
     Mobile Responsive
     ============================================ */
  @media (max-width: 640px) {
    .settings-page .page-header h1 {
      font-size: 24px;
    }

    .settings-section {
      margin-bottom: 24px;
    }

    .settings-card {
      padding: 16px;
      border-radius: 12px;
    }

    .setting-row {
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
    }

    .theme-selector {
      width: 100%;
      justify-content: flex-start;
    }

    .theme-option {
      flex: 1;
      justify-content: center;
    }

    .device-name-row {
      flex-direction: column;
      align-items: flex-start;
      gap: 4px;
    }

    /* Metrics cards mobile */
    .metrics-card {
      border-radius: 12px;
    }

    .metrics-card-header {
      padding: 14px 16px;
    }

    .metrics-card-body {
      padding: 14px 16px;
    }

    /* Thresholds mobile */
    .threshold-item {
      flex-wrap: wrap;
      gap: 8px;
    }

    .threshold-label {
      min-width: 80px;
      font-size: 12px;
    }

    .threshold-slider-wrap {
      min-width: 100px;
      order: 3;
      flex-basis: 100%;
      margin-top: 4px;
    }

    .threshold-input {
      width: 48px;
      height: 28px;
      font-size: 13px;
    }


    /* Alert sections mobile */
    .alerts-body {
      gap: 16px;
    }

    .alert-metrics-row {
      gap: 6px;
    }

    .alert-metric-check {
      padding: 8px 10px;
      font-size: 12px;
    }

    .sensitivity-chips {
      gap: 8px;
    }

    .sensitivity-chip {
      padding: 12px 10px;
    }

    .notify-chips {
      gap: 6px;
    }

    .notify-chip {
      padding: 10px 8px;
      font-size: 11px;
    }

    .notify-chip svg {
      width: 14px;
      height: 14px;
    }

    .alert-options {
      padding: 10px 12px;
    }

    .alert-option-row {
      flex-wrap: wrap;
    }

    .cooldown-chips {
      gap: 3px;
    }

    .cooldown-chip {
      padding: 5px 8px;
      font-size: 10px;
    }

    /* Data options mobile */
    .data-option {
      gap: 12px;
    }

    .data-option-name {
      font-size: 13px;
    }

    .data-option-desc {
      font-size: 11px;
    }

    /* Toggle size */
    .toggle {
      width: 40px;
      height: 22px;
    }

    .toggle-slider:before {
      width: 16px;
      height: 16px;
    }

    .toggle input:checked + .toggle-slider:before {
      transform: translateX(18px);
    }
  }
</style>
