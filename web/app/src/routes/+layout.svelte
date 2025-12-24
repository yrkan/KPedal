<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/stores';
  import { auth, isAuthenticated, isLoading, user } from '$lib/auth';
  import { theme } from '$lib/theme';
  import '../app.css';

  let mounted = false;

  onMount(() => {
    auth.initialize();
    mounted = true;
  });

  $: currentPath = $page.url.pathname;
</script>

<svelte:head>
  <title>KPedal</title>
</svelte:head>

{#if !mounted || $isLoading}
  <div class="loading-screen">
    <div class="loading-content">
      <div class="loading-logo">
        <span class="loading-dot"></span>
        <span class="loading-text">KPedal</span>
      </div>
      <div class="spinner"></div>
    </div>
  </div>
{:else}
  {#if $isAuthenticated}
    <nav class="navbar">
      <div class="nav-container">
        <a href="/" class="logo">
          <span class="logo-dot"></span>
          <span class="logo-text">KPedal</span>
        </a>

        <div class="nav-links">
          <a href="/" class="nav-link" class:active={currentPath === '/'} aria-label="Dashboard">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
              <polyline points="9,22 9,12 15,12 15,22"/>
            </svg>
            <span>Dashboard</span>
          </a>
          <a href="/rides" class="nav-link" class:active={currentPath === '/rides'} aria-label="Ride history">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
            </svg>
            <span>Rides</span>
          </a>
          <a href="/settings" class="nav-link" class:active={currentPath === '/settings'} aria-label="Settings">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/>
              <circle cx="12" cy="12" r="3"/>
            </svg>
            <span>Settings</span>
          </a>
        </div>

        <div class="nav-actions">
          <button class="theme-toggle" on:click={() => theme.toggle()} title="Toggle theme" aria-label="Toggle theme">
            {#if $theme === 'dark' || ($theme === 'system' && typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches)}
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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
            {:else}
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
              </svg>
            {/if}
          </button>

          <div class="user-menu">
            {#if $user?.picture}
              <img src={$user.picture} alt="" class="avatar" referrerpolicy="no-referrer" />
            {:else}
              <div class="avatar-placeholder">{$user?.name?.[0] || '?'}</div>
            {/if}
          </div>
        </div>
      </div>
    </nav>
  {/if}

  <main class="main-content" class:with-nav={$isAuthenticated}>
    <slot />
  </main>
{/if}

<style>
  .loading-screen {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
    background: var(--bg-base);
  }

  .loading-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 24px;
  }

  .loading-logo {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .loading-dot {
    width: 10px;
    height: 10px;
    background: var(--color-optimal);
    border-radius: 50%;
  }

  .loading-text {
    font-size: 20px;
    font-weight: 700;
    color: var(--text-primary);
    letter-spacing: -0.3px;
  }

  /* Navbar */
  .navbar {
    position: sticky;
    top: 0;
    z-index: 100;
    background: var(--bg-surface);
    border-bottom: 1px solid var(--border-subtle);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
  }

  .nav-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 24px;
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 32px;
  }

  .logo {
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--text-primary);
    font-weight: 600;
    font-size: 18px;
  }

  .logo:hover {
    color: var(--text-primary);
  }

  .logo-dot {
    width: 8px;
    height: 8px;
    background: var(--color-optimal);
    border-radius: 50%;
  }

  .logo-text {
    font-size: 16px;
    letter-spacing: -0.3px;
    font-weight: 700;
  }

  .nav-links {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .nav-link {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    border-radius: 8px;
    color: var(--text-secondary);
    font-size: 14px;
    font-weight: 500;
    transition: all 0.2s ease;
  }

  .nav-link:hover {
    color: var(--text-primary);
    background: var(--bg-hover);
  }

  .nav-link.active {
    color: var(--text-primary);
    background: var(--bg-elevated);
  }

  .nav-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .theme-toggle {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    background: transparent;
    color: var(--text-secondary);
  }

  .theme-toggle:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
  }

  .user-menu {
    display: flex;
    align-items: center;
  }

  .avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    object-fit: cover;
  }

  .avatar-placeholder {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: var(--bg-elevated);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-secondary);
  }

  .main-content {
    min-height: 100vh;
  }

  .main-content.with-nav {
    min-height: calc(100vh - 64px);
  }

  /* Mobile */
  @media (max-width: 768px) {
    .nav-link span {
      display: none;
    }
    .nav-link {
      padding: 10px;
    }
    .logo-text {
      display: none;
    }
  }
</style>
