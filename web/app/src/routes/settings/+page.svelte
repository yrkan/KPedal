<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { auth, isAuthenticated, user } from '$lib/auth';
  import { theme, type Theme } from '$lib/theme';

  onMount(() => {
    if (!$isAuthenticated) {
      goto('/login');
    }
  });

  function setTheme(newTheme: Theme) {
    theme.set(newTheme);
  }
</script>

<svelte:head>
  <title>Settings - KPedal</title>
</svelte:head>

<div class="settings-page">
  <div class="container container-md">
    <header class="page-header animate-in">
      <h1>Settings</h1>
    </header>

    <!-- Account -->
    <section class="settings-section animate-in">
      <div class="section-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
          <circle cx="12" cy="7" r="4"/>
        </svg>
        <h2>Account</h2>
      </div>
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
    </section>

    <!-- Appearance -->
    <section class="settings-section animate-in">
      <div class="section-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="3"/>
          <path d="M12 1v2m0 18v2M4.22 4.22l1.42 1.42m12.72 12.72 1.42 1.42M1 12h2m18 0h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
        </svg>
        <h2>Appearance</h2>
      </div>
      <div class="settings-card">
        <div class="setting-row">
          <div class="setting-info">
            <span class="setting-label">Theme</span>
            <span class="setting-description">Choose your preferred color scheme</span>
          </div>
          <div class="theme-selector">
            <button
              class="theme-option"
              class:active={$theme === 'light'}
              on:click={() => setTheme('light')}
              aria-label="Light theme"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="5"/>
                <line x1="12" y1="1" x2="12" y2="3"/>
                <line x1="12" y1="21" x2="12" y2="23"/>
                <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/>
                <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
                <line x1="1" y1="12" x2="3" y2="12"/>
                <line x1="21" y1="12" x2="23" y2="12"/>
                <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/>
                <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
              </svg>
              Light
            </button>
            <button
              class="theme-option"
              class:active={$theme === 'dark'}
              on:click={() => setTheme('dark')}
              aria-label="Dark theme"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
              </svg>
              Dark
            </button>
            <button
              class="theme-option"
              class:active={$theme === 'system'}
              on:click={() => setTheme('system')}
              aria-label="System theme"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="2" y="3" width="20" height="14" rx="2"/>
                <line x1="8" y1="21" x2="16" y2="21"/>
                <line x1="12" y1="17" x2="12" y2="21"/>
              </svg>
              System
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- Data -->
    <section class="settings-section animate-in">
      <div class="section-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21.21 15.89A10 10 0 1 1 8 2.83"/>
          <path d="M22 12A10 10 0 0 0 12 2v10z"/>
        </svg>
        <h2>Data</h2>
      </div>
      <div class="settings-card">
        <div class="setting-row">
          <div class="setting-info">
            <span class="setting-label">Ride Data</span>
            <span class="setting-description">Your data is stored securely and synced from your Karoo device</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Session -->
    <section class="settings-section animate-in">
      <div class="section-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
        </svg>
        <h2>Session</h2>
      </div>
      <div class="settings-card">
        <div class="setting-row">
          <div class="setting-info">
            <span class="setting-label">Signed in via Google</span>
            <span class="setting-description">Your session will expire in 7 days</span>
          </div>
          <button class="btn btn-danger btn-sm" on:click={() => auth.logout()}>
            Sign Out
          </button>
        </div>
      </div>
    </section>

    <!-- Privacy -->
    <section class="settings-section animate-in">
      <div class="section-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
          <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
        </svg>
        <h2>Privacy</h2>
      </div>
      <div class="settings-card">
        <a href="/privacy" class="setting-row setting-link">
          <div class="setting-info">
            <span class="setting-label">Privacy Policy</span>
            <span class="setting-description">Learn how we collect, use, and protect your data</span>
          </div>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9,6 15,12 9,18"/>
          </svg>
        </a>
      </div>
    </section>

    <!-- About -->
    <section class="settings-section animate-in">
      <div class="section-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/>
          <path d="M12 16v-4"/>
          <circle cx="12" cy="8" r="0.5" fill="currentColor"/>
        </svg>
        <h2>About</h2>
      </div>
      <div class="settings-card">
        <div class="about-content">
          <p class="app-name">KPedal Web</p>
          <p class="app-version">Version 1.0.0</p>
          <p class="app-description">Pedaling efficiency analytics for Karoo</p>
        </div>
      </div>
    </section>
  </div>
</div>

<style>
  .settings-page {
    padding: 32px 0 64px;
  }

  .page-header {
    margin-bottom: 32px;
  }

  .page-header h1 {
    font-size: 28px;
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: -0.5px;
  }

  .settings-section {
    margin-bottom: 32px;
  }

  .section-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
  }

  .section-header svg {
    color: var(--text-muted);
  }

  .settings-section h2 {
    font-size: 12px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: var(--text-tertiary);
  }

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

  .setting-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
  }

  .setting-link {
    text-decoration: none;
    transition: background 0.15s ease;
    margin: -20px;
    padding: 20px;
    border-radius: 16px;
  }

  .setting-link:hover {
    background: var(--bg-hover);
  }

  .setting-link svg {
    color: var(--text-muted);
    transition: color 0.15s ease;
  }

  .setting-link:hover svg {
    color: var(--text-secondary);
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
    .setting-row {
      flex-direction: column;
      align-items: flex-start;
    }
    .theme-selector {
      width: 100%;
      justify-content: flex-start;
    }
  }
</style>
