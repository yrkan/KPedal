<script lang="ts">
  import { t } from '$lib/i18n';
  import SettingsSection from './SettingsSection.svelte';

  const icon = `<circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><circle cx="12" cy="8" r="0.5" fill="currentColor"/>`;
</script>

<SettingsSection title={$t('settings.about')} {icon}>
  <div class="settings-card">
    <div class="about-content">
      <p class="app-name">KPedal Web</p>
      <p class="app-version">Version 1.7.2</p>
      <p class="app-description">{$t('app.tagline')}</p>
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

  @media (max-width: 640px) {
    .settings-card {
      padding: 16px;
      border-radius: 12px;
    }
  }
</style>
