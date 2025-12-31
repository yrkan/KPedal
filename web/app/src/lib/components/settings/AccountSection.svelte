<script lang="ts">
  import { t } from '$lib/i18n';
  import { user } from '$lib/auth';
  import SettingsSection from './SettingsSection.svelte';

  const icon = `<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>`;
</script>

<SettingsSection title={$t('settings.account')} {icon}>
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
</SettingsSection>

<style>
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

  @media (max-width: 640px) {
    .settings-card {
      padding: 16px;
      border-radius: 12px;
    }
  }
</style>
