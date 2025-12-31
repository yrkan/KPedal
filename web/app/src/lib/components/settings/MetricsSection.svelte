<script lang="ts">
  import { t } from '$lib/i18n';
  import { authFetch } from '$lib/auth';
  import InfoTip from '$lib/components/InfoTip.svelte';
  import type { KPedalSettings } from '$lib/types/settings';
  import { DEFAULT_SETTINGS } from '$lib/types/settings';
  import SettingsSection from './SettingsSection.svelte';

  export let settings: KPedalSettings = { ...DEFAULT_SETTINGS };
  export let loading = true;
  export let error = false;
  export let onSave: (changes: Partial<KPedalSettings>) => void;

  const icon = `<path d="M12 20v-6M6 20V10M18 20V4"/>`;

  export async function loadSettings() {
    loading = true;
    error = false;
    try {
      const res = await authFetch('/settings');
      const data = await res.json();
      if (data.success && data.data?.settings) {
        settings = { ...DEFAULT_SETTINGS, ...data.data.settings };
      }
    } catch (err) {
      error = true;
    } finally {
      loading = false;
    }
  }

  function save(changes: Partial<KPedalSettings>) {
    settings = { ...settings, ...changes };
    onSave(changes);
  }
</script>

<SettingsSection title={$t('settings.pedalingMetrics')} {icon}>
  <!-- Sync Banner -->
  <div class="sync-info-banner">
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M21 12a9 9 0 0 1-9 9m9-9a9 9 0 0 0-9-9m9 9H3m9 9a9 9 0 0 1-9-9m9 9c-1.657 0-3-4.03-3-9s1.343-9 3-9m0 18c1.657 0 3-4.03 3-9s-1.343-9-3-9"/>
    </svg>
    <span>{@html $t('settings.syncBanner')}</span>
  </div>

  {#if loading}
    <div class="metrics-card">
      <div class="loading-state">
        <div class="spinner-small"></div>
        <span>{$t('common.loading')}</span>
      </div>
    </div>
  {:else if error}
    <div class="metrics-card">
      <div class="error-state">
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
                disabled={settings.balance_threshold <= 1}
                on:click={() => save({ balance_threshold: Math.max(1, settings.balance_threshold - 1) })}
              >−</button>
              <span class="threshold-value">±{settings.balance_threshold}%</span>
              <button
                class="threshold-btn"
                disabled={settings.balance_threshold >= 10}
                on:click={() => save({ balance_threshold: Math.min(10, settings.balance_threshold + 1) })}
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
                  disabled={settings.te_optimal_min <= 50}
                  on:click={() => save({ te_optimal_min: Math.max(50, settings.te_optimal_min - 5) })}
                >−</button>
                <span class="threshold-value">{settings.te_optimal_min}%</span>
                <button
                  class="threshold-btn"
                  disabled={settings.te_optimal_min >= settings.te_optimal_max - 5}
                  on:click={() => save({ te_optimal_min: Math.min(settings.te_optimal_max - 5, settings.te_optimal_min + 5) })}
                >+</button>
              </div>
              <span class="threshold-range-sep">–</span>
              <div class="threshold-input-group small">
                <button
                  class="threshold-btn"
                  disabled={settings.te_optimal_max <= settings.te_optimal_min + 5}
                  on:click={() => save({ te_optimal_max: Math.max(settings.te_optimal_min + 5, settings.te_optimal_max - 5) })}
                >−</button>
                <span class="threshold-value">{settings.te_optimal_max}%</span>
                <button
                  class="threshold-btn"
                  disabled={settings.te_optimal_max >= 100}
                  on:click={() => save({ te_optimal_max: Math.min(100, settings.te_optimal_max + 5) })}
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
                disabled={settings.ps_minimum <= 10}
                on:click={() => save({ ps_minimum: Math.max(10, settings.ps_minimum - 5) })}
              >−</button>
              <span class="threshold-value">{settings.ps_minimum}%</span>
              <button
                class="threshold-btn"
                disabled={settings.ps_minimum >= 30}
                on:click={() => save({ ps_minimum: Math.min(30, settings.ps_minimum + 5) })}
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
                checked={settings.alerts_enabled}
                on:change={(e) => save({ alerts_enabled: e.currentTarget.checked })}
              />
              <span class="toggle-slider"></span>
            </label>
          </div>
        </div>
        {#if settings.alerts_enabled}
          <div class="metrics-card-body alerts-body">
            <!-- Metrics to Monitor -->
            <div class="alert-section">
              <span class="alert-section-label">{$t('settings.metricsToMonitor')}</span>
              <div class="alert-metrics-row">
                <label class="alert-metric-check" class:active={settings.balance_alert_enabled}>
                  <input type="checkbox" checked={settings.balance_alert_enabled}
                    on:change={() => save({ balance_alert_enabled: !settings.balance_alert_enabled })} />
                  <span class="metric-dot" style="background: var(--color-attention)"></span>
                  <span>{$t('metrics.balance')}</span>
                </label>
                <label class="alert-metric-check" class:active={settings.te_alert_enabled}>
                  <input type="checkbox" checked={settings.te_alert_enabled}
                    on:change={() => save({ te_alert_enabled: !settings.te_alert_enabled })} />
                  <span class="metric-dot" style="background: var(--color-optimal)"></span>
                  <span>{$t('settings.torqueEff')}</span>
                </label>
                <label class="alert-metric-check" class:active={settings.ps_alert_enabled}>
                  <input type="checkbox" checked={settings.ps_alert_enabled}
                    on:change={() => save({ ps_alert_enabled: !settings.ps_alert_enabled })} />
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
                  class:active={settings.balance_alert_trigger === 'PROBLEM_ONLY'}
                  on:click={() => save({
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
                  class:active={settings.balance_alert_trigger === 'ATTENTION_AND_PROBLEM'}
                  on:click={() => save({
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
                  class:active={settings.balance_alert_vibration}
                  on:click={() => save({
                    balance_alert_vibration: !settings.balance_alert_vibration,
                    te_alert_vibration: !settings.balance_alert_vibration,
                    ps_alert_vibration: !settings.balance_alert_vibration
                  })}
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                  </svg>
                  {$t('settings.vibrate')}
                </button>
                <button
                  class="notify-chip"
                  class:active={settings.balance_alert_sound}
                  on:click={() => save({
                    balance_alert_sound: !settings.balance_alert_sound,
                    te_alert_sound: !settings.balance_alert_sound,
                    ps_alert_sound: !settings.balance_alert_sound
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
                  class:active={settings.balance_alert_visual}
                  on:click={() => save({
                    balance_alert_visual: !settings.balance_alert_visual,
                    te_alert_visual: !settings.balance_alert_visual,
                    ps_alert_visual: !settings.balance_alert_visual
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
                    <input type="checkbox" checked={settings.screen_wake_on_alert}
                      on:change={(e) => save({ screen_wake_on_alert: e.currentTarget.checked })} />
                    <span class="toggle-slider"></span>
                  </label>
                </div>
                <div class="alert-option-row">
                  <span class="alert-option-label">{$t('settings.cooldownBetweenAlerts')}</span>
                  <div class="cooldown-chips">
                    {#each [{ value: 15, label: '15s' }, { value: 30, label: '30s' }, { value: 60, label: '1m' }, { value: 120, label: '2m' }] as opt}
                      <button
                        class="cooldown-chip"
                        class:active={settings.balance_alert_cooldown === opt.value}
                        on:click={() => save({
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
                checked={settings.background_mode_enabled}
                on:change={(e) => save({ background_mode_enabled: e.currentTarget.checked })}
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
                checked={settings.auto_sync_enabled}
                on:change={(e) => save({ auto_sync_enabled: e.currentTarget.checked })}
              />
              <span class="toggle-slider"></span>
            </label>
          </div>
        </div>
      </div>
    </div>
  {/if}
</SettingsSection>

<style>
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

  .sync-info-banner :global(strong) {
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

  .loading-state,
  .error-state {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    padding: 24px;
    color: var(--text-secondary);
    font-size: 14px;
  }

  .error-state {
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

  .spinner-small {
    width: 18px;
    height: 18px;
    border: 2px solid var(--border-default);
    border-top-color: var(--color-accent);
    border-radius: 50%;
    animation: spin 0.7s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  /* Thresholds */
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

  /* Alerts */
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

  /* Mobile */
  @media (max-width: 640px) {
    .metrics-card {
      border-radius: 12px;
    }

    .metrics-card-header {
      padding: 14px 16px;
    }

    .metrics-card-body {
      padding: 14px 16px;
    }

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

    .data-option {
      gap: 12px;
    }

    .data-option-name {
      font-size: 13px;
    }

    .data-option-desc {
      font-size: 11px;
    }

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
</style>
