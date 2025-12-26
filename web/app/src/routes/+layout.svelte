<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/stores';
  import { auth, isAuthenticated, isLoading, user, isDemo } from '$lib/auth';
  import { startDashboardTour, resetTour, hasPendingTour, continueTourOnPage } from '$lib/tour';
  import { theme } from '$lib/theme';
  import '../app.css';

  let mounted = false;

  function handleRestartTour() {
    resetTour();
    // Only start tour on dashboard - redirect if on other page
    if ($page.url.pathname === '/') {
      startDashboardTour();
    } else {
      // Navigate to dashboard to start tour from beginning
      window.location.href = '/';
    }
  }

  // Handle tour continuation when navigating between pages
  $: if (mounted && $isAuthenticated && $isDemo && hasPendingTour()) {
    // Delay to let page render
    setTimeout(() => {
      continueTourOnPage($page.url.pathname);
    }, 300);
  }

  onMount(() => {
    // Skip initialize on auth callback pages - they handle auth themselves
    const isAuthCallback = $page.url.pathname === '/auth/success';
    if (!isAuthCallback) {
      auth.initialize();
    }
    mounted = true;
  });

  $: currentPath = $page.url.pathname;
</script>

<svelte:head>
  <title>KPedal — Pedaling Efficiency Analytics for Karoo</title>
  <meta name="description" content="Real-time pedaling analytics for Hammerhead Karoo. Track power balance, torque effectiveness, and pedal smoothness with guided drills and cloud sync.">
</svelte:head>

{#if !mounted || ($isLoading && currentPath !== '/auth/success')}
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
    <!-- Top navbar -->
    <nav class="navbar">
      <div class="nav-container">
        <a href="/" class="logo">
          <span class="logo-dot"></span>
          <span class="logo-text">KPedal</span>
        </a>

        <div class="nav-links desktop-only">
          <a href="/" class="nav-link" class:active={currentPath === '/'}>Dashboard</a>
          <a href="/rides" class="nav-link" class:active={currentPath === '/rides'}>Rides</a>
          <a href="/drills" class="nav-link" class:active={currentPath === '/drills'}>Drills</a>
          <a href="/achievements" class="nav-link" class:active={currentPath === '/achievements'}>Achievements</a>
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

          <a href="/settings" class="avatar-link" title="Settings">
            {#if $user?.picture}
              <img src={$user.picture} alt="" class="avatar" referrerpolicy="no-referrer" />
            {:else}
              <div class="avatar-placeholder">{$user?.name?.[0] || '?'}</div>
            {/if}
          </a>
        </div>
      </div>
    </nav>

    <!-- Bottom navigation (mobile only) -->
    <nav class="bottom-nav mobile-only">
      <a href="/" class="bottom-nav-item" class:active={currentPath === '/'}>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>
        </svg>
        <span>Home</span>
      </a>
      <a href="/rides" class="bottom-nav-item" class:active={currentPath === '/rides'}>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
        </svg>
        <span>Rides</span>
      </a>
      <a href="/drills" class="bottom-nav-item" class:active={currentPath === '/drills'}>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/><polyline points="12,6 12,12 16,14"/>
        </svg>
        <span>Drills</span>
      </a>
      <a href="/achievements" class="bottom-nav-item" class:active={currentPath === '/achievements'}>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polygon points="12,2 15.09,8.26 22,9.27 17,14.14 18.18,21.02 12,17.77 5.82,21.02 7,14.14 2,9.27 8.91,8.26"/>
        </svg>
        <span>Goals</span>
      </a>
      <a href="/settings" class="bottom-nav-item" class:active={currentPath === '/settings'}>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
        </svg>
        <span>Profile</span>
      </a>
    </nav>
  {/if}

  <main class="main-content" class:with-nav={$isAuthenticated}>
    {#if $isDemo}
      <div class="demo-banner">
        <div class="demo-banner-content">
          <span class="demo-badge">Demo Mode</span>
          <span class="demo-text">Viewing sample data. <a href="https://github.com/yrkan/kpedal/releases" target="_blank" rel="noopener">Download KPedal</a> to see your own rides.</span>
        </div>
        <button class="demo-tour-btn" on:click={handleRestartTour}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polygon points="10,8 16,12 10,16" fill="currentColor" stroke="none"/>
          </svg>
          Tour
        </button>
      </div>
    {/if}
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

  /* Bottom Navigation */
  .bottom-nav {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    z-index: 100;
    display: flex;
    justify-content: space-around;
    align-items: center;
    height: 64px;
    background: var(--bg-base);
    border-top: 1px solid var(--border-subtle);
    padding-bottom: env(safe-area-inset-bottom);
  }

  .bottom-nav-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 8px 12px;
    color: var(--text-muted);
    text-decoration: none;
    transition: color 0.15s;
  }

  .bottom-nav-item svg {
    width: 22px;
    height: 22px;
  }

  .bottom-nav-item span {
    font-size: 10px;
    font-weight: 500;
  }

  .bottom-nav-item.active {
    color: var(--text-primary);
  }

  .bottom-nav-item.active svg {
    stroke-width: 2.5;
  }

  /* Responsive visibility */
  .desktop-only { display: flex; }
  .mobile-only { display: none; }

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

  .avatar-link {
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    transition: opacity 0.15s ease;
  }

  .avatar-link:hover {
    opacity: 0.8;
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

  /* Demo Banner */
  .demo-banner {
    background: var(--bg-elevated);
    border-bottom: 1px solid var(--border-subtle);
    padding: 10px 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 16px;
    flex-wrap: wrap;
  }

  .demo-banner-content {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    justify-content: center;
  }

  .demo-badge {
    background: var(--color-attention-soft);
    color: var(--color-attention-text);
    padding: 3px 8px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }

  .demo-text {
    font-size: 13px;
    color: var(--text-secondary);
  }

  .demo-text a {
    color: var(--color-accent);
    text-decoration: underline;
    text-underline-offset: 2px;
  }

  .demo-text a:hover {
    color: var(--color-accent-hover);
  }

  /* Mobile */
  @media (max-width: 768px) {
    .desktop-only { display: none; }
    .mobile-only { display: flex; }

    .nav-container {
      padding: 0 16px;
      height: 48px;
    }

    .main-content.with-nav {
      min-height: calc(100vh - 48px);
      padding-bottom: 72px;
    }

    .demo-banner {
      padding: 8px 16px;
      gap: 10px;
    }

    .demo-text {
      font-size: 12px;
    }
  }
</style>
