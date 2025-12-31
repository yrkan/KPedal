<script lang="ts">
  import { t } from '$lib/i18n';
  import { auth } from '$lib/auth';
  import SettingsSection from './SettingsSection.svelte';

  const icon = `<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>`;
</script>

<SettingsSection title={$t('settings.session')} {icon}>
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
