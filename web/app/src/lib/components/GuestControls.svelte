<script lang="ts">
  import { theme, resolvedTheme } from '$lib/theme';
  import { t, locale, locales, localeNames, setLocale, type Locale } from '$lib/i18n';
</script>

<div class="guest-controls">
  <select
    class="lang-select"
    value={$locale}
    on:change={(e) => setLocale(e.currentTarget.value as Locale)}
    aria-label={$t('aria.languageSelector')}
  >
    {#each locales as loc}
      <option value={loc}>{localeNames[loc]}</option>
    {/each}
  </select>

  <button class="theme-toggle" on:click={() => theme.toggle()} aria-label={$t('common.toggleTheme')}>
    {#if $resolvedTheme === 'light'}
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="5"/>
        <line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
        <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
        <line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>
        <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
      </svg>
    {:else}
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
      </svg>
    {/if}
    {#if $theme === 'auto'}
      <span class="auto-badge">A</span>
    {/if}
  </button>
</div>

<style>
  .guest-controls {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .lang-select {
    appearance: none;
    height: 36px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 8px;
    padding: 0 28px 0 12px;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-secondary);
    cursor: pointer;
    transition: all 0.15s ease;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%236b7280' stroke-width='2'%3E%3Cpolyline points='6,9 12,15 18,9'/%3E%3C/svg%3E");
    background-repeat: no-repeat;
    background-position: right 8px center;
  }

  .lang-select:hover {
    border-color: var(--border-default);
    color: var(--text-primary);
  }

  .lang-select:focus {
    outline: none;
    border-color: var(--color-optimal);
  }

  .lang-select option {
    background: var(--bg-surface);
    color: var(--text-primary);
  }

  .theme-toggle {
    position: relative;
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    color: var(--text-secondary);
    cursor: pointer;
    transition: all 0.15s ease;
    flex-shrink: 0;
  }

  .theme-toggle:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
    border-color: var(--color-optimal);
  }

  .auto-badge {
    position: absolute;
    bottom: -4px;
    right: -4px;
    width: 14px;
    height: 14px;
    background: #22c55e;
    color: #fff;
    font-size: 9px;
    font-weight: 900;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    line-height: 1;
    box-shadow: 0 1px 3px rgba(0,0,0,0.4);
    border: 1.5px solid var(--bg-base);
  }

  @media (max-width: 640px) {
    .guest-controls {
      gap: 6px;
    }

    .lang-select {
      height: 34px;
      font-size: 12px;
      padding: 0 24px 0 10px;
    }

    .theme-toggle {
      width: 34px;
      height: 34px;
    }

    .theme-toggle svg {
      width: 16px;
      height: 16px;
    }
  }
</style>
