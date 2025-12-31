<script lang="ts">
  import { t } from '$lib/i18n';
  import { authFetch } from '$lib/auth';
  import type { Device } from '$lib/types/settings';
  import SettingsSection from './SettingsSection.svelte';

  export let devices: Device[] = [];
  export let loading = true;
  export let error = false;

  let removingDevice: string | null = null;
  let requestingSyncDevice: string | null = null;
  let syncRequestedDevice: string | null = null;

  const icon = `<rect x="4" y="2" width="16" height="20" rx="3"/><circle cx="12" cy="17" r="1.5"/><line x1="8" y1="6" x2="16" y2="6"/><line x1="8" y1="9" x2="14" y2="9"/>`;

  export async function loadDevices() {
    loading = true;
    error = false;
    try {
      const res = await authFetch('/devices');
      const data = await res.json();
      if (data.success) {
        devices = data.data.devices;
      } else {
        error = true;
      }
    } catch (err) {
      error = true;
    } finally {
      loading = false;
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
</script>

<SettingsSection title={$t('settings.connectedDevices')} {icon}>
  <div class="settings-card">
    {#if loading}
      <div class="devices-loading">
        <div class="spinner-small"></div>
        <span>{$t('settings.loadingDevices')}</span>
      </div>
    {:else if error}
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
</SettingsSection>

<style>
  .settings-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 16px;
    padding: 20px;
  }

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

  .device-sync,
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

  .device-sync:hover:not(:disabled) {
    background: var(--color-accent-soft);
    color: var(--color-accent);
  }

  .device-sync:disabled {
    opacity: 0.7;
    cursor: not-allowed;
    color: var(--color-optimal-text);
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

  @media (max-width: 640px) {
    .settings-card {
      padding: 16px;
      border-radius: 12px;
    }

    .device-name-row {
      flex-direction: column;
      align-items: flex-start;
      gap: 4px;
    }
  }
</style>
