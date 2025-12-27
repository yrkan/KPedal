<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { isAuthenticated, auth } from '$lib/auth';
  import { theme } from '$lib/theme';
  import { API_URL } from '$lib/config';

  let error: string | null = null;
  let demoLoading = false;

  // Redirect to app.kpedal.com if on kpedal.com (landing domain)
  function redirectToDashboard() {
    if (typeof window !== 'undefined' && window.location.hostname === 'kpedal.com') {
      window.location.href = 'https://app.kpedal.com/';
    } else {
      goto('/');
    }
  }

  onMount(() => {
    if ($isAuthenticated) {
      redirectToDashboard();
      return;
    }

    const errorParam = $page.url.searchParams.get('error');
    if (errorParam) {
      switch (errorParam) {
        case 'invalid_state':
          error = 'Security verification failed. Please try again.';
          break;
        case 'auth_failed':
          error = 'Authentication failed. Please try again.';
          break;
        default:
          error = 'Something went wrong. Please try again.';
      }
    }
  });

  function handleLogin() {
    window.location.href = `${API_URL}/auth/login`;
  }

  async function handleDemoLogin() {
    demoLoading = true;
    error = null;
    try {
      const success = await auth.demoLogin();
      if (success) {
        redirectToDashboard();
      } else {
        error = 'Demo mode temporarily unavailable. Please try again later.';
      }
    } catch {
      error = 'Failed to start demo mode. Please try again.';
    } finally {
      demoLoading = false;
    }
  }
</script>

<svelte:head>
  <title>Sign In — KPedal Pedaling Analytics</title>
  <meta name="description" content="Sign in to KPedal to access your pedaling analytics dashboard. View ride history, track balance improvements, and sync data across devices.">
  <meta name="robots" content="noindex, nofollow">
  <link rel="canonical" href="https://kpedal.com/login">
</svelte:head>

<div class="login-page">
  <div class="bg-gradient"></div>
  <div class="bg-pattern"></div>

  <div class="login-container">
    <button class="theme-toggle" on:click={() => theme.toggle()} aria-label="Toggle theme">
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

    <div class="login-card">
      <div class="logo">
        <span class="logo-dot"></span>
        <span class="logo-text">KPedal</span>
      </div>

      <p class="tagline">Pedaling efficiency analytics for Karoo</p>

      {#if error}
        <div class="error-banner">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
          {error}
        </div>
      {/if}

      <button class="google-btn" on:click={handleLogin}>
        <svg viewBox="0 0 24 24" width="18" height="18">
          <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
          <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
          <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
          <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
        </svg>
        Continue with Google
      </button>

      <div class="divider">
        <span>or</span>
      </div>

      <button class="demo-btn" on:click={handleDemoLogin} disabled={demoLoading}>
        {#if demoLoading}
          <span class="spinner"></span>
          Loading demo...
        {:else}
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polygon points="10 8 16 12 10 16 10 8" fill="currentColor" stroke="none"/>
          </svg>
          Try Demo
        {/if}
      </button>

      <p class="demo-note">Explore the dashboard with sample data.<br>No sign-up required.</p>

      <div class="trust-section">
        <div class="trust-row">
          <div class="trust-item">
            <div class="trust-icon">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
            </div>
            <span>Secure OAuth 2.0</span>
          </div>
          <div class="trust-item">
            <div class="trust-icon check">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </div>
            <span>Google verified</span>
          </div>
        </div>
      </div>

      <p class="privacy-note">
        By signing in, you agree to our <a href="/privacy">Privacy Policy</a>
      </p>
    </div>

    <div class="footer">
      <a href="https://kpedal.com" class="landing-link">← back to landing page</a>
    </div>
  </div>
</div>

<style>
  .login-page {
    min-height: 100dvh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
    background: var(--bg-base);
    position: relative;
    overflow: hidden;
  }

  .bg-gradient {
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle at 30% 20%, var(--color-optimal-soft) 0%, transparent 40%),
                radial-gradient(circle at 70% 80%, var(--color-optimal-soft) 0%, transparent 30%);
    opacity: 0.5;
    pointer-events: none;
  }

  .bg-pattern {
    position: absolute;
    inset: 0;
    background-image: radial-gradient(var(--border-subtle) 1px, transparent 1px);
    background-size: 24px 24px;
    opacity: 0.5;
    pointer-events: none;
  }

  .login-container {
    width: 100%;
    max-width: 360px;
    position: relative;
    z-index: 1;
  }

  .theme-toggle {
    position: absolute;
    top: -52px;
    right: 0;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 10px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    color: var(--text-secondary);
    cursor: pointer;
    transition: all 0.15s ease;
  }

  .theme-toggle:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
  }

  .login-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 24px;
    padding: 40px 32px;
    text-align: center;
    box-shadow: var(--shadow-lg);
  }

  .logo {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    margin-bottom: 8px;
  }

  .logo-dot {
    width: 12px;
    height: 12px;
    background: var(--color-optimal);
    border-radius: 50%;
    box-shadow: 0 0 8px var(--color-optimal);
  }

  .logo-text {
    font-size: 26px;
    font-weight: 700;
    color: var(--text-primary);
    letter-spacing: -0.5px;
  }

  .tagline {
    font-size: 14px;
    color: var(--text-tertiary);
    margin: 0 0 32px 0;
  }

  .error-banner {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px 16px;
    background: var(--color-problem-soft);
    color: var(--color-problem-text);
    border-radius: 12px;
    font-size: 13px;
    margin-bottom: 20px;
  }

  .google-btn {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    padding: 14px 24px;
    background: var(--bg-elevated);
    border: 1px solid var(--border-default);
    border-radius: 12px;
    font-size: 15px;
    font-weight: 500;
    color: var(--text-primary);
    cursor: pointer;
    transition: all 0.15s ease;
  }

  .google-btn:hover {
    background: var(--bg-hover);
    border-color: var(--border-strong);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  .divider {
    display: flex;
    align-items: center;
    gap: 12px;
    margin: 16px 0;
  }

  .divider::before,
  .divider::after {
    content: '';
    flex: 1;
    height: 1px;
    background: var(--border-subtle);
  }

  .divider span {
    font-size: 12px;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .demo-btn {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    padding: 14px 24px;
    background: var(--color-optimal);
    border: none;
    border-radius: 12px;
    font-size: 15px;
    font-weight: 500;
    color: white;
    cursor: pointer;
    transition: all 0.2s ease;
    position: relative;
    overflow: hidden;
    box-shadow: 0 2px 8px var(--glow-optimal, rgba(34, 197, 94, 0.3));
  }

  :global([data-theme="dark"]) .demo-btn {
    background: #16a34a;
    box-shadow: 0 2px 8px rgba(22, 163, 74, 0.25);
  }

  .demo-btn::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
    transition: left 0.5s ease;
  }

  .demo-btn:hover:not(:disabled)::before {
    left: 100%;
  }

  .demo-btn:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 4px 14px var(--glow-optimal, rgba(34, 197, 94, 0.4));
    filter: brightness(1.05);
  }

  :global([data-theme="dark"]) .demo-btn:hover:not(:disabled) {
    background: #15803d;
    box-shadow: 0 4px 14px rgba(22, 163, 74, 0.35);
  }

  .demo-btn:active:not(:disabled) {
    transform: translateY(0);
  }

  .demo-btn:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }

  .spinner {
    width: 16px;
    height: 16px;
    border: 2px solid transparent;
    border-top-color: currentColor;
    border-radius: 50%;
    animation: spin 0.7s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  .demo-note {
    margin: 12px 0 0 0;
    font-size: 12px;
    color: var(--text-muted);
    text-align: center;
    line-height: 1.6;
  }

  .privacy-note {
    margin: 16px 0 0 0;
    padding-top: 16px;
    border-top: 1px solid var(--border-subtle);
    font-size: 12px;
    color: var(--text-muted);
  }

  .privacy-note a {
    color: var(--text-secondary);
    text-decoration: none;
  }

  .privacy-note a:hover {
    text-decoration: underline;
  }

  .trust-section {
    margin-top: 20px;
    padding-top: 16px;
    border-top: 1px solid var(--border-subtle);
  }

  .trust-row {
    display: flex;
    justify-content: center;
    gap: 16px;
  }

  .trust-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 11px;
    color: var(--text-secondary);
  }

  .trust-icon {
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-elevated);
    border-radius: 6px;
    color: var(--text-muted);
  }

  .trust-icon.check {
    background: var(--color-optimal-soft);
    color: var(--color-optimal-text);
  }

  .footer {
    margin-top: 24px;
    display: flex;
    justify-content: flex-start;
  }

  .landing-link {
    font-size: 13px;
    color: var(--text-muted);
    text-decoration: none;
  }

  .landing-link:hover {
    text-decoration: underline;
  }

  @media (max-width: 400px) {
    .login-card {
      padding: 32px 24px;
      border-radius: 20px;
    }

    .logo-text {
      font-size: 24px;
    }
  }
</style>
