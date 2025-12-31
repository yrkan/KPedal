<script lang="ts">
  import { t } from '$lib/i18n';
  import SettingsSection from './SettingsSection.svelte';

  const icon = `<path d="M21.21 15.89A10 10 0 1 1 8 2.83"/><path d="M22 12A10 10 0 0 0 12 2v10z"/>`;
</script>

<SettingsSection title={$t('settings.data')} {icon}>
  <div class="settings-card">
    <div class="setting-row">
      <div class="setting-info">
        <span class="setting-label">{$t('settings.rideData')}</span>
        <span class="setting-description">{$t('settings.rideDataHint')}</span>
      </div>
    </div>
  </div>
</SettingsSection>

<style>
  .settings-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 16px;
    padding: 20px;
  }

  .setting-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
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

  @media (max-width: 640px) {
    .settings-card {
      padding: 16px;
      border-radius: 12px;
    }
  }
</style>
