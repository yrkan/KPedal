<script lang="ts">
  import { t } from '$lib/i18n';
  import { theme, type Theme } from '$lib/theme';
  import SettingsSection from './SettingsSection.svelte';

  const icon = `<circle cx="12" cy="12" r="3"/><path d="M12 1v2m0 18v2M4.22 4.22l1.42 1.42m12.72 12.72 1.42 1.42M1 12h2m18 0h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>`;

  function setTheme(newTheme: Theme) {
    theme.set(newTheme);
  }
</script>

<SettingsSection title={$t('settings.appearance')} {icon}>
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
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 4L5 20M12 4L19 20M8 14h8"/>
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

  @media (max-width: 640px) {
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
  }
</style>
