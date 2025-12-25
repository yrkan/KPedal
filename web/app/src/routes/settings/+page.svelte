<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { auth, isAuthenticated, user, authFetch } from '$lib/auth';
  import { theme, type Theme } from '$lib/theme';

  interface Device {
    id: string;
    name: string;
    type: string;
    status: 'connected' | 'idle' | 'offline';
    last_sync: string | null;
    last_sync_relative: string | null;
    created_at: string;
  }

  interface KPedalSettings {
    balance_threshold: number;
    te_optimal_min: number;
    te_optimal_max: number;
    ps_minimum: number;
    alerts_enabled: boolean;
    screen_wake_on_alert: boolean;
    balance_alert_enabled: boolean;
    balance_alert_trigger: 'PROBLEM_ONLY' | 'ATTENTION_AND_PROBLEM';
    balance_alert_vibration: boolean;
    balance_alert_sound: boolean;
    balance_alert_cooldown: number;
    te_alert_enabled: boolean;
    te_alert_trigger: 'PROBLEM_ONLY' | 'ATTENTION_AND_PROBLEM';
    te_alert_vibration: boolean;
    te_alert_sound: boolean;
    te_alert_cooldown: number;
    ps_alert_enabled: boolean;
    ps_alert_trigger: 'PROBLEM_ONLY' | 'ATTENTION_AND_PROBLEM';
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
    balance_alert_vibration: true,
    balance_alert_sound: false,
    balance_alert_cooldown: 30,
    te_alert_enabled: true,
    te_alert_trigger: 'PROBLEM_ONLY',
    te_alert_vibration: true,
    te_alert_sound: false,
    te_alert_cooldown: 30,
    ps_alert_enabled: true,
    ps_alert_trigger: 'PROBLEM_ONLY',
    ps_alert_vibration: true,
    ps_alert_sound: false,
    ps_alert_cooldown: 30,
    background_mode_enabled: true,
    auto_sync_enabled: true,
  };

  let devices: Device[] = [];
  let devicesLoading = true;
  let devicesError: string | null = null;
  let removingDevice: string | null = null;
  let requestingSyncDevice: string | null = null;
  let syncRequestedDevice: string | null = null;

  // KPedal settings state
  let kpedalSettings: KPedalSettings = { ...DEFAULT_SETTINGS };
  let settingsLoading = true;
  let settingsError: string | null = null;
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
    devicesError = null;
    try {
      const res = await authFetch('/api/devices');
      const data = await res.json();
      if (data.success) {
        devices = data.data.devices;
      } else {
        devicesError = data.error || 'Failed to load devices';
      }
    } catch (err) {
      devicesError = 'Failed to connect';
    } finally {
      devicesLoading = false;
    }
  }

  async function removeDevice(deviceId: string) {
    if (!confirm('Remove this device? It will need to be re-linked.')) return;

    removingDevice = deviceId;
    try {
      const res = await authFetch(`/api/devices/${deviceId}`, { method: 'DELETE' });
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
      const res = await authFetch(`/api/devices/${deviceId}/request-sync`, { method: 'POST' });
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
      case 'connected': return 'Connected';
      case 'idle': return 'Idle';
      default: return 'Offline';
    }
  }

  // Load KPedal settings from server
  async function loadSettings() {
    settingsLoading = true;
    settingsError = null;
    try {
      const res = await authFetch('/api/settings');
      const data = await res.json();
      if (data.success && data.data?.settings) {
        kpedalSettings = { ...DEFAULT_SETTINGS, ...data.data.settings };
      }
    } catch (err) {
      settingsError = 'Failed to load settings';
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
        const res = await authFetch('/api/settings', {
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
  <title>Settings - KPedal</title>
</svelte:head>

<div class="settings-page">
  <div class="container container-md">
    <header class="page-header animate-in">
      <h1>Settings</h1>
    </header>

    <!-- Account -->
    <section class="settings-section animate-in">
      <div class="section-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
          <circle cx="12" cy="7" r="4"/>
        </svg>
        <h2>Account</h2>
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
          <rect x="2" y="3" width="20" height="14" rx="2"/>
          <line x1="8" y1="21" x2="16" y2="21"/>
          <line x1="12" y1="17" x2="12" y2="21"/>
        </svg>
        <h2>Connected Devices</h2>
      </div>
      <div class="settings-card">
        {#if devicesLoading}
          <div class="devices-loading">
            <div class="spinner-small"></div>
            <span>Loading devices...</span>
          </div>
        {:else if devicesError}
          <div class="devices-error">
            <span>{devicesError}</span>
            <button class="text-btn" on:click={loadDevices}>Retry</button>
          </div>
        {:else if devices.length === 0}
          <div class="devices-empty">
            <div class="empty-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="2" y="3" width="20" height="14" rx="2"/>
                <line x1="8" y1="21" x2="16" y2="21"/>
                <line x1="12" y1="17" x2="12" y2="21"/>
              </svg>
            </div>
            <p class="empty-text">No devices connected</p>
            <p class="empty-hint">Link your Karoo device to sync ride data</p>
            <a href="/link" class="link-device-btn">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
              Link Device
            </a>
          </div>
        {:else}
          <div class="devices-list">
            {#each devices as device (device.id)}
              <div class="device-item">
                <div class="device-icon">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="2" y="3" width="20" height="14" rx="2"/>
                    <line x1="8" y1="21" x2="16" y2="21"/>
                    <line x1="12" y1="17" x2="12" y2="21"/>
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
                      <span class="sync-requested">Sync requested - open app to sync</span>
                    {:else if device.last_sync_relative}
                      <span>Last sync: {device.last_sync_relative}</span>
                    {:else}
                      <span>Never synced</span>
                    {/if}
                  </div>
                </div>
                <button
                  class="device-sync"
                  on:click={() => requestSync(device.id)}
                  disabled={requestingSyncDevice === device.id || syncRequestedDevice === device.id}
                  title="Request sync"
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
                  title="Remove device"
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
              Link another device
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
        <h2>Pedaling Metrics</h2>
        {#if savingSettings}
          <span class="save-indicator saving">Saving...</span>
        {:else if settingsSaved}
          <span class="save-indicator saved">Saved</span>
        {/if}
      </div>

      {#if settingsLoading}
        <div class="settings-card">
          <div class="devices-loading">
            <div class="spinner-small"></div>
            <span>Loading...</span>
          </div>
        </div>
      {:else if settingsError}
        <div class="settings-card">
          <div class="devices-error">
            <span>{settingsError}</span>
            <button class="text-btn" on:click={loadSettings}>Retry</button>
          </div>
        </div>
      {:else}
        <div class="metrics-settings">
          <!-- Thresholds Card -->
          <div class="metrics-card">
            <div class="metrics-card-header">
              <h3>Thresholds</h3>
            </div>
            <div class="metrics-card-body thresholds-body">
              <!-- Balance -->
              <div class="threshold-item">
                <span class="threshold-label">Balance</span>
                <div class="threshold-slider-wrap">
                  <input
                    type="range"
                    class="threshold-slider gradient-rg"
                    min="1"
                    max="10"
                    step="1"
                    value={kpedalSettings.balance_threshold}
                    on:input={(e) => saveSettings({ balance_threshold: parseInt(e.currentTarget.value) })}
                    style="--val: {(kpedalSettings.balance_threshold - 1) / 9}"
                  />
                  <div class="threshold-tooltip" style="--val: {(kpedalSettings.balance_threshold - 1) / 9}">
                    ±{kpedalSettings.balance_threshold}%
                  </div>
                </div>
                <input
                  type="number"
                  class="threshold-input"
                  min="1"
                  max="10"
                  value={kpedalSettings.balance_threshold}
                  on:change={(e) => {
                    const val = Math.max(1, Math.min(10, parseInt(e.currentTarget.value) || 5));
                    e.currentTarget.value = String(val);
                    saveSettings({ balance_threshold: val });
                  }}
                />
                <span class="threshold-unit">%</span>
              </div>

              <!-- TE Min -->
              <div class="threshold-item">
                <span class="threshold-label">TE Min</span>
                <div class="threshold-slider-wrap">
                  <input
                    type="range"
                    class="threshold-slider gradient-gr"
                    min="50"
                    max="85"
                    step="5"
                    value={kpedalSettings.te_optimal_min}
                    on:input={(e) => saveSettings({ te_optimal_min: parseInt(e.currentTarget.value) })}
                    style="--val: {(kpedalSettings.te_optimal_min - 50) / 35}"
                  />
                  <div class="threshold-tooltip" style="--val: {(kpedalSettings.te_optimal_min - 50) / 35}">
                    {kpedalSettings.te_optimal_min}%
                  </div>
                </div>
                <input
                  type="number"
                  class="threshold-input"
                  min="50"
                  max="85"
                  step="5"
                  value={kpedalSettings.te_optimal_min}
                  on:change={(e) => {
                    const val = Math.max(50, Math.min(85, parseInt(e.currentTarget.value) || 70));
                    e.currentTarget.value = String(val);
                    saveSettings({ te_optimal_min: val });
                  }}
                />
                <span class="threshold-unit">%</span>
              </div>

              <!-- TE Max -->
              <div class="threshold-item">
                <span class="threshold-label">TE Max</span>
                <div class="threshold-slider-wrap">
                  <input
                    type="range"
                    class="threshold-slider gradient-gr"
                    min="65"
                    max="100"
                    step="5"
                    value={kpedalSettings.te_optimal_max}
                    on:input={(e) => saveSettings({ te_optimal_max: parseInt(e.currentTarget.value) })}
                    style="--val: {(kpedalSettings.te_optimal_max - 65) / 35}"
                  />
                  <div class="threshold-tooltip" style="--val: {(kpedalSettings.te_optimal_max - 65) / 35}">
                    {kpedalSettings.te_optimal_max}%
                  </div>
                </div>
                <input
                  type="number"
                  class="threshold-input"
                  min="65"
                  max="100"
                  step="5"
                  value={kpedalSettings.te_optimal_max}
                  on:change={(e) => {
                    const val = Math.max(65, Math.min(100, parseInt(e.currentTarget.value) || 80));
                    e.currentTarget.value = String(val);
                    saveSettings({ te_optimal_max: val });
                  }}
                />
                <span class="threshold-unit">%</span>
              </div>

              <!-- PS -->
              <div class="threshold-item">
                <span class="threshold-label">Smoothness</span>
                <div class="threshold-slider-wrap">
                  <input
                    type="range"
                    class="threshold-slider gradient-gr"
                    min="10"
                    max="30"
                    step="5"
                    value={kpedalSettings.ps_minimum}
                    on:input={(e) => saveSettings({ ps_minimum: parseInt(e.currentTarget.value) })}
                    style="--val: {(kpedalSettings.ps_minimum - 10) / 20}"
                  />
                  <div class="threshold-tooltip" style="--val: {(kpedalSettings.ps_minimum - 10) / 20}">
                    {kpedalSettings.ps_minimum}%
                  </div>
                </div>
                <input
                  type="number"
                  class="threshold-input"
                  min="10"
                  max="30"
                  step="5"
                  value={kpedalSettings.ps_minimum}
                  on:change={(e) => {
                    const val = Math.max(10, Math.min(30, parseInt(e.currentTarget.value) || 20));
                    e.currentTarget.value = String(val);
                    saveSettings({ ps_minimum: val });
                  }}
                />
                <span class="threshold-unit">%</span>
              </div>
            </div>
          </div>

          <!-- Alerts Card -->
          <div class="metrics-card">
            <div class="metrics-card-header">
              <div class="header-with-toggle">
                <div>
                  <h3>In-Ride Alerts</h3>
                  <span class="metrics-card-hint">Notifications when metrics need attention</span>
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
              <div class="metrics-card-body">
                <div class="alert-metrics-grid">
                  <button
                    class="alert-metric-btn"
                    class:active={kpedalSettings.balance_alert_enabled}
                    on:click={() => saveSettings({ balance_alert_enabled: !kpedalSettings.balance_alert_enabled })}
                  >
                    <span class="alert-metric-icon">
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z"/>
                        <path d="M12 6v6l4 2"/>
                      </svg>
                    </span>
                    <span class="alert-metric-name">Balance</span>
                    <span class="alert-metric-status">{kpedalSettings.balance_alert_enabled ? 'On' : 'Off'}</span>
                  </button>
                  <button
                    class="alert-metric-btn"
                    class:active={kpedalSettings.te_alert_enabled}
                    on:click={() => saveSettings({ te_alert_enabled: !kpedalSettings.te_alert_enabled })}
                  >
                    <span class="alert-metric-icon">
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
                      </svg>
                    </span>
                    <span class="alert-metric-name">Torque Eff.</span>
                    <span class="alert-metric-status">{kpedalSettings.te_alert_enabled ? 'On' : 'Off'}</span>
                  </button>
                  <button
                    class="alert-metric-btn"
                    class:active={kpedalSettings.ps_alert_enabled}
                    on:click={() => saveSettings({ ps_alert_enabled: !kpedalSettings.ps_alert_enabled })}
                  >
                    <span class="alert-metric-icon">
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"/>
                        <path d="M12 16v-4M12 8h.01"/>
                      </svg>
                    </span>
                    <span class="alert-metric-name">Smoothness</span>
                    <span class="alert-metric-status">{kpedalSettings.ps_alert_enabled ? 'On' : 'Off'}</span>
                  </button>
                </div>
                <div class="alert-options-row">
                  <label class="option-check">
                    <input
                      type="checkbox"
                      checked={kpedalSettings.balance_alert_vibration && kpedalSettings.te_alert_vibration && kpedalSettings.ps_alert_vibration}
                      on:change={(e) => saveSettings({
                        balance_alert_vibration: e.currentTarget.checked,
                        te_alert_vibration: e.currentTarget.checked,
                        ps_alert_vibration: e.currentTarget.checked
                      })}
                    />
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                      <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
                    </svg>
                    <span>Vibration</span>
                  </label>
                  <label class="option-check">
                    <input
                      type="checkbox"
                      checked={kpedalSettings.screen_wake_on_alert}
                      on:change={(e) => saveSettings({ screen_wake_on_alert: e.currentTarget.checked })}
                    />
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="2" y="3" width="20" height="14" rx="2"/>
                      <line x1="8" y1="21" x2="16" y2="21"/>
                      <line x1="12" y1="17" x2="12" y2="21"/>
                    </svg>
                    <span>Wake screen</span>
                  </label>
                </div>
              </div>
            {/if}
          </div>

          <!-- Data Collection Card -->
          <div class="metrics-card">
            <div class="metrics-card-header">
              <h3>Data Collection</h3>
              <span class="metrics-card-hint">How KPedal collects and syncs your data</span>
            </div>
            <div class="metrics-card-body">
              <div class="data-option">
                <div class="data-option-info">
                  <span class="data-option-name">Background Mode</span>
                  <span class="data-option-desc">Collect metrics even when data field is hidden</span>
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
                  <span class="data-option-name">Auto-Sync</span>
                  <span class="data-option-desc">Sync ride data automatically after each ride</span>
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
        <h2>Appearance</h2>
      </div>
      <div class="settings-card">
        <div class="setting-row">
          <div class="setting-info">
            <span class="setting-label">Theme</span>
            <span class="setting-description">Choose your preferred color scheme</span>
          </div>
          <div class="theme-selector">
            <button
              class="theme-option"
              class:active={$theme === 'light'}
              on:click={() => setTheme('light')}
              aria-label="Light theme"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="5"/>
                <line x1="12" y1="1" x2="12" y2="3"/>
                <line x1="12" y1="21" x2="12" y2="23"/>
                <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/>
                <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
                <line x1="1" y1="12" x2="3" y2="12"/>
                <line x1="21" y1="12" x2="23" y2="12"/>
                <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/>
                <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
              </svg>
              Light
            </button>
            <button
              class="theme-option"
              class:active={$theme === 'dark'}
              on:click={() => setTheme('dark')}
              aria-label="Dark theme"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
              </svg>
              Dark
            </button>
            <button
              class="theme-option"
              class:active={$theme === 'system'}
              on:click={() => setTheme('system')}
              aria-label="System theme"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="2" y="3" width="20" height="14" rx="2"/>
                <line x1="8" y1="21" x2="16" y2="21"/>
                <line x1="12" y1="17" x2="12" y2="21"/>
              </svg>
              System
            </button>
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
        <h2>Data</h2>
      </div>
      <div class="settings-card">
        <div class="setting-row">
          <div class="setting-info">
            <span class="setting-label">Ride Data</span>
            <span class="setting-description">Your data is stored securely and synced from your Karoo device</span>
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
        <h2>Session</h2>
      </div>
      <div class="settings-card">
        <div class="setting-row">
          <div class="setting-info">
            <span class="setting-label">Signed in via Google</span>
            <span class="setting-description">Your session will expire in 7 days</span>
          </div>
          <button class="btn btn-danger btn-sm" on:click={() => auth.logout()}>
            Sign Out
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
        <h2>Privacy</h2>
      </div>
      <div class="settings-card">
        <a href="/privacy" class="setting-row setting-link">
          <div class="setting-info">
            <span class="setting-label">Privacy Policy</span>
            <span class="setting-description">Learn how we collect, use, and protect your data</span>
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
        <h2>About</h2>
      </div>
      <div class="settings-card">
        <div class="about-content">
          <p class="app-name">KPedal Web</p>
          <p class="app-version">Version 1.0.0</p>
          <p class="app-description">Pedaling efficiency analytics for Karoo</p>
        </div>
      </div>
    </section>
  </div>
</div>

<style>
  .settings-page {
    padding: 32px 0 64px;
  }

  .page-header {
    margin-bottom: 32px;
  }

  .page-header h1 {
    font-size: 28px;
    font-weight: 600;
    color: var(--text-primary);
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
  .save-indicator {
    margin-left: auto;
    font-size: 11px;
    font-weight: 500;
    padding: 2px 8px;
    border-radius: 10px;
  }

  .save-indicator.saving {
    color: var(--text-secondary);
    background: var(--bg-elevated);
  }

  .save-indicator.saved {
    color: var(--color-optimal-text);
    background: var(--color-optimal-soft);
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

  /* Thresholds - Compact with gradients */
  .thresholds-body {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .threshold-item {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .threshold-label {
    font-size: 13px;
    font-weight: 500;
    color: var(--text-primary);
    min-width: 90px;
    flex-shrink: 0;
  }

  .threshold-slider-wrap {
    position: relative;
    flex: 1;
    min-width: 120px;
  }

  .threshold-slider {
    width: 100%;
    height: 8px;
    -webkit-appearance: none;
    border-radius: 4px;
    outline: none;
    cursor: pointer;
  }

  /* Gradient: red → yellow → green (for Balance - lower is better) */
  .threshold-slider.gradient-rg {
    background: linear-gradient(to right,
      var(--color-optimal) 0%,
      var(--color-attention) 50%,
      var(--color-problem) 100%);
  }

  /* Gradient: red → yellow → green (for PS - higher is better) */
  .threshold-slider.gradient-gr {
    background: linear-gradient(to right,
      var(--color-problem) 0%,
      var(--color-attention) 50%,
      var(--color-optimal) 100%);
  }

  .threshold-slider::-webkit-slider-thumb {
    -webkit-appearance: none;
    width: 20px;
    height: 20px;
    background: var(--bg-surface);
    border: 3px solid var(--text-primary);
    border-radius: 50%;
    cursor: pointer;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    transition: transform 0.15s, border-color 0.15s;
  }

  .threshold-slider::-webkit-slider-thumb:hover {
    transform: scale(1.1);
  }

  .threshold-slider::-moz-range-thumb {
    width: 20px;
    height: 20px;
    background: var(--bg-surface);
    border: 3px solid var(--text-primary);
    border-radius: 50%;
    cursor: pointer;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  }

  .threshold-tooltip {
    position: absolute;
    top: -28px;
    left: calc(var(--val) * 100%);
    transform: translateX(-50%);
    background: var(--bg-elevated);
    border: 1px solid var(--border-default);
    color: var(--text-primary);
    font-size: 11px;
    font-weight: 600;
    padding: 3px 8px;
    border-radius: 6px;
    white-space: nowrap;
    pointer-events: none;
    opacity: 0;
    transition: opacity 0.15s;
  }

  .threshold-slider-wrap:hover .threshold-tooltip {
    opacity: 1;
  }

  .threshold-input {
    width: 52px;
    height: 32px;
    background: var(--bg-elevated);
    border: 1px solid var(--border-default);
    border-radius: 6px;
    color: var(--text-primary);
    font-size: 14px;
    font-weight: 500;
    text-align: center;
    outline: none;
    transition: border-color 0.15s, box-shadow 0.15s;
    -moz-appearance: textfield;
  }

  .threshold-input::-webkit-outer-spin-button,
  .threshold-input::-webkit-inner-spin-button {
    -webkit-appearance: none;
    margin: 0;
  }

  .threshold-input:focus {
    border-color: var(--color-accent);
    box-shadow: 0 0 0 3px var(--color-accent-soft);
  }

  .threshold-unit {
    font-size: 13px;
    color: var(--text-tertiary);
    min-width: 16px;
  }


  /* Alert Metrics Grid */
  .alert-metrics-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
    margin-bottom: 16px;
  }

  .alert-metric-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 16px 12px;
    background: var(--bg-elevated);
    border: 2px solid var(--border-subtle);
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.2s;
  }

  .alert-metric-btn:hover {
    border-color: var(--border-default);
    background: var(--bg-hover);
  }

  .alert-metric-btn.active {
    border-color: var(--color-optimal);
    background: var(--color-optimal-soft);
  }

  .alert-metric-icon {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-surface);
    border-radius: 10px;
    color: var(--text-secondary);
    transition: all 0.2s;
  }

  .alert-metric-btn.active .alert-metric-icon {
    background: var(--color-accent);
    color: var(--color-accent-text);
  }

  .alert-metric-name {
    font-size: 13px;
    font-weight: 500;
    color: var(--text-primary);
  }

  .alert-metric-status {
    font-size: 11px;
    color: var(--text-tertiary);
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }

  .alert-metric-btn.active .alert-metric-status {
    color: var(--color-optimal-text);
  }

  .alert-options-row {
    display: flex;
    gap: 24px;
    padding-top: 16px;
    border-top: 1px solid var(--border-subtle);
  }

  .option-check {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    color: var(--text-secondary);
    transition: color 0.15s;
  }

  .option-check:hover {
    color: var(--text-primary);
  }

  .option-check input[type="checkbox"] {
    display: none;
  }

  .option-check svg {
    opacity: 0.5;
    transition: opacity 0.15s;
  }

  .option-check:has(input:checked) svg {
    opacity: 1;
    color: var(--color-optimal-text);
  }

  .option-check span {
    font-size: 13px;
  }

  .option-check:has(input:checked) span {
    color: var(--text-primary);
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
    .settings-page {
      padding: 24px 0 48px;
    }

    .page-header {
      margin-bottom: 24px;
    }

    .page-header h1 {
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


    /* Alert grid mobile */
    .alert-metrics-grid {
      grid-template-columns: repeat(3, 1fr);
      gap: 8px;
    }

    .alert-metric-btn {
      padding: 12px 8px;
    }

    .alert-metric-icon {
      width: 32px;
      height: 32px;
    }

    .alert-metric-icon svg {
      width: 16px;
      height: 16px;
    }

    .alert-metric-name {
      font-size: 11px;
    }

    .alert-metric-status {
      font-size: 10px;
    }

    .alert-options-row {
      gap: 16px;
    }

    .option-check span {
      font-size: 12px;
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
