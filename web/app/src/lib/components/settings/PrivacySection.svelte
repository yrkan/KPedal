<script lang="ts">
  import { t } from '$lib/i18n';
  import SettingsSection from './SettingsSection.svelte';

  const icon = `<rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>`;
</script>

<SettingsSection title={$t('settings.privacy')} {icon}>
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

  .setting-link {
    text-decoration: none;
    color: inherit;
    transition: background 0.2s ease;
    margin: -20px;
    padding: 20px;
    border-radius: 16px;
  }

  .setting-link:hover {
    background: var(--bg-hover);
  }

  .setting-link svg {
    color: var(--text-muted);
    flex-shrink: 0;
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

    .setting-link {
      margin: -16px;
      padding: 16px;
      border-radius: 12px;
    }
  }
</style>
