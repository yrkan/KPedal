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

  async function handleDemoSignup() {
    // Logout from demo first, then redirect to login
    await auth.logout();
    window.location.href = '/login';
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
          <span class="demo-text">Viewing sample data</span>
        </div>
        <div class="demo-actions">
          <button class="demo-signup-btn" on:click={handleDemoSignup}>
            <svg viewBox="0 0 24 24" width="16" height="16">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
            </svg>
            Create Free Account
          </button>
          <button class="demo-tour-btn" on:click={handleRestartTour}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <polygon points="10,8 16,12 10,16" fill="currentColor" stroke="none"/>
            </svg>
            Tour
          </button>
        </div>
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

  .demo-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .demo-signup-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 12px;
    background: var(--bg-base);
    border: 1px solid var(--border-default);
    border-radius: 6px;
    font-size: 12px;
    font-weight: 500;
    color: var(--text-primary);
    cursor: pointer;
    transition: all 0.15s ease;
  }

  .demo-signup-btn:hover {
    background: var(--bg-hover);
    border-color: var(--border-strong);
  }

  .demo-tour-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 6px 10px;
    background: transparent;
    border: 1px solid var(--border-subtle);
    border-radius: 6px;
    font-size: 12px;
    font-weight: 500;
    color: var(--text-secondary);
    cursor: pointer;
    transition: all 0.15s ease;
  }

  .demo-tour-btn:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
  }

  .demo-tour-btn svg {
    width: 14px;
    height: 14px;
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
      gap: 8px;
    }

    .demo-text {
      font-size: 12px;
    }

    .demo-actions {
      gap: 6px;
    }

    .demo-signup-btn {
      padding: 5px 10px;
      font-size: 11px;
    }

    .demo-tour-btn {
      padding: 5px 8px;
      font-size: 11px;
    }
  }

  /* Small mobile */
  @media (max-width: 480px) {
    .demo-banner {
      padding: 8px 12px;
      gap: 8px;
    }

    .demo-banner-content {
      display: none;
    }

    .demo-actions {
      width: 100%;
      justify-content: center;
      gap: 8px;
    }

    .demo-signup-btn {
      padding: 6px 12px;
      font-size: 11px;
    }

    .demo-tour-btn {
      padding: 6px 10px;
      font-size: 11px;
    }
  }
</style>
