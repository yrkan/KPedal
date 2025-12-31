<script lang="ts">
  import { t, locale, locales, localeNames, setLocale, type Locale } from '$lib/i18n';
  import { theme, resolvedTheme } from '$lib/theme';
</script>

<header class="landing-header">
  <a href="/" class="site-logo" aria-label={$t('aria.home')}>
    <span class="site-logo-dot" aria-hidden="true"></span>
    <span class="site-logo-text">KPedal</span>
  </a>

  <div class="header-actions">
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
</header>

<style>
  .landing-header {
    /* Semantic wrapper for fixed header elements */
    display: contents;
  }

  .site-logo {
    position: fixed;
    top: 32px;
    left: 32px;
    display: flex;
    align-items: center;
    gap: 8px;
    text-decoration: none;
    z-index: 100;
  }
  .site-logo-dot {
    width: 8px;
    height: 8px;
    background: var(--color-optimal);
    border-radius: 50%;
  }
  .site-logo-text {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: -0.5px;
  }

  .header-actions {
    position: fixed;
    top: 32px;
    right: 32px;
    display: flex;
    align-items: center;
    gap: 12px;
    z-index: 100;
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
    backdrop-filter: blur(12px);
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
    border-color: var(--color-accent);
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
    backdrop-filter: blur(12px);
  }
  .theme-toggle:hover {
    border-color: var(--border-default);
    color: var(--text-secondary);
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

  /* Responsive - 768px */
  @media (max-width: 768px) {
    .site-logo { top: 16px; left: 16px; }
    .header-actions {
      top: 16px;
      right: 16px;
      gap: 8px;
    }
    .lang-select {
      height: 34px;
      padding: 0 26px 0 10px;
      font-size: 12px;
    }
    .theme-toggle {
      width: 34px;
      height: 34px;
    }
  }

  /* Responsive - 480px */
  @media (max-width: 480px) {
    .site-logo {
      top: 16px;
      left: 20px;
      gap: 8px;
    }
    .site-logo-dot {
      width: 8px;
      height: 8px;
      box-shadow: 0 0 8px var(--color-optimal);
    }
    .site-logo-text {
      font-size: 15px;
      font-weight: 600;
      letter-spacing: -0.3px;
    }
    .header-actions {
      top: 16px;
      right: 20px;
      gap: 6px;
    }
    .lang-select {
      height: 32px;
      padding: 0 24px 0 10px;
      font-size: 12px;
    }
    .theme-toggle {
      width: 32px;
      height: 32px;
    }
    .theme-toggle svg {
      width: 16px;
      height: 16px;
    }
  }

  /* Responsive - 360px */
  @media (max-width: 360px) {
    .site-logo { top: 12px; left: 12px; }
    .site-logo-text { font-size: 13px; }
    .theme-toggle { top: 12px; right: 12px; width: 32px; height: 32px; }
  }
</style>
