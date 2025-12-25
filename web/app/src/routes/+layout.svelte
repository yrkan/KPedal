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
          <a href="/" class="nav-link" class:active={currentPath === '/'}>Dashboard</a>
          <a href="/rides" class="nav-link" class:active={currentPath === '/rides'}>Rides</a>
          <a href="/settings" class="nav-link" class:active={currentPath === '/settings'}>Settings</a>
        </div>

        <div class="nav-right">
          <button class="icon-btn" on:click={() => theme.toggle()} title="Toggle theme">
            {#if $theme === 'dark'}
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
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
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
              </svg>
            {/if}
          </button>

          {#if $user?.picture}
            <img src={$user.picture} alt="" class="avatar" referrerpolicy="no-referrer" />
          {:else}
            <div class="avatar-placeholder">{$user?.name?.[0] || '?'}</div>
          {/if}
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
    background: var(--bg-base);
    border-bottom: 1px solid var(--border-subtle);
  }

  .nav-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 24px;
    height: 52px;
    display: flex;
    align-items: center;
    gap: 48px;
  }

  .logo {
    display: flex;
    align-items: center;
    gap: 8px;
    text-decoration: none;
  }

  .logo-dot {
    width: 8px;
    height: 8px;
    background: var(--color-optimal);
    border-radius: 50%;
  }

  .logo-text {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: -0.3px;
  }

  .nav-links {
    display: flex;
    align-items: center;
    gap: 24px;
  }

  .nav-link {
    color: var(--text-secondary);
    font-size: 14px;
    font-weight: 500;
    text-decoration: none;
    transition: color 0.15s ease;
  }

  .nav-link:hover {
    color: var(--text-primary);
  }

  .nav-link.active {
    color: var(--text-primary);
  }

  .nav-right {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-left: auto;
  }

  .icon-btn {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 6px;
    background: transparent;
    border: none;
    color: var(--text-secondary);
    cursor: pointer;
    transition: all 0.15s ease;
  }

  .icon-btn:hover {
    color: var(--text-primary);
    background: var(--bg-hover);
  }

  .avatar {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    object-fit: cover;
  }

  .avatar-placeholder {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: var(--bg-elevated);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 600;
    color: var(--text-secondary);
  }

  .main-content {
    min-height: 100vh;
  }

  .main-content.with-nav {
    min-height: calc(100vh - 52px);
  }

  /* Mobile */
  @media (max-width: 640px) {
    .nav-container {
      padding: 0 16px;
      gap: 24px;
    }

    .nav-links {
      gap: 16px;
    }

    .nav-link {
      font-size: 13px;
    }

    .logo-text {
      display: none;
    }
  }
</style>
