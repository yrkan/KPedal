<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { isAuthenticated } from '$lib/auth';
  import { theme } from '$lib/theme';
  import { API_URL } from '$lib/config';

  let error: string | null = null;

  onMount(() => {
    if ($isAuthenticated) {
      goto('/');
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
</script>

<svelte:head>
  <title>Sign in - KPedal</title>
</svelte:head>

<div class="login-page">
  <div class="login-container">
    <!-- Theme toggle -->
    <button class="theme-toggle" on:click={() => theme.toggle()} aria-label="Toggle theme">
      {#if $theme === 'dark' || ($theme === 'system' && typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches)}
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
        </svg>
      {/if}
    </button>

    <div class="login-card">
      <!-- Logo -->
      <div class="logo-section">
        <div class="logo">
          <span class="logo-dot"></span>
          <span class="logo-text">KPedal</span>
        </div>
        <p>Pedaling efficiency analytics for your Karoo</p>
      </div>

      {#if error}
        <div class="error-banner">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/>
            <path d="M12 8v4"/>
            <circle cx="12" cy="16" r="0.5" fill="currentColor"/>
          </svg>
          {error}
        </div>
      {/if}

      <!-- Sign in button -->
      <button class="google-btn" on:click={handleLogin}>
        <svg viewBox="0 0 24 24" width="20" height="20">
          <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
          <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
          <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
          <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
        </svg>
        Continue with Google
      </button>

      <!-- Features -->
      <div class="features">
        <div class="feature">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
          </svg>
          <span>Secure & Private</span>
        </div>
        <div class="feature">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21.21 15.89A10 10 0 1 1 8 2.83"/>
            <path d="M22 12A10 10 0 0 0 12 2v10z"/>
          </svg>
          <span>Detailed Analytics</span>
        </div>
        <div class="feature">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12a9 9 0 0 1-9 9m9-9a9 9 0 0 0-9-9m9 9H3m9 9a9 9 0 0 1-9-9m9 9c1.66 0 3-4.03 3-9s-1.34-9-3-9m0 18c-1.66 0-3-4.03-3-9s1.34-9 3-9m-9 9a9 9 0 0 1 9-9"/>
          </svg>
          <span>Cross-device Sync</span>
        </div>
      </div>
    </div>

    <p class="footer-text">
      By signing in, you agree to our <a href="/privacy">Privacy Policy</a>.
      <br>Your ride data is stored securely and never shared.
    </p>
  </div>
</div>

<style>
  .login-page {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    background: var(--bg-base);
  }

  .login-container {
    width: 100%;
    max-width: 420px;
    position: relative;
  }

  .theme-toggle {
    position: absolute;
    top: -60px;
    right: 0;
    width: 44px;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 12px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    color: var(--text-secondary);
    cursor: pointer;
    transition: all 0.2s ease;
  }

  .theme-toggle:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
  }

  .login-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 24px;
    padding: 48px 40px;
    box-shadow: var(--shadow-lg);
  }

  .logo-section {
    text-align: center;
    margin-bottom: 32px;
  }

  .logo {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    margin-bottom: 16px;
  }

  .logo-dot {
    width: 10px;
    height: 10px;
    background: var(--color-optimal);
    border-radius: 50%;
  }

  .logo-text {
    font-size: 24px;
    font-weight: 700;
    color: var(--text-primary);
    letter-spacing: -0.5px;
  }

  .logo-section p {
    font-size: 15px;
    color: var(--text-secondary);
  }

  .error-banner {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 14px 16px;
    background: var(--color-problem-soft);
    color: var(--color-problem);
    border-radius: 12px;
    font-size: 14px;
    margin-bottom: 24px;
  }

  .google-btn {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    padding: 14px 24px;
    background: var(--bg-elevated);
    border: 1px solid var(--border-default);
    border-radius: 12px;
    font-size: 15px;
    font-weight: 500;
    color: var(--text-primary);
    cursor: pointer;
    transition: all 0.2s ease;
  }

  .google-btn:hover {
    background: var(--bg-hover);
    border-color: var(--border-strong);
    transform: translateY(-1px);
    box-shadow: var(--shadow-md);
  }

  .features {
    display: flex;
    justify-content: center;
    gap: 24px;
    margin-top: 32px;
    padding-top: 32px;
    border-top: 1px solid var(--border-subtle);
  }

  .feature {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    color: var(--text-tertiary);
    font-size: 12px;
    text-align: center;
  }

  .feature svg {
    color: var(--text-muted);
  }

  .footer-text {
    text-align: center;
    font-size: 12px;
    color: var(--text-muted);
    margin-top: 24px;
    line-height: 1.6;
  }

  @media (max-width: 480px) {
    .login-card {
      padding: 32px 24px;
    }
    .features {
      flex-direction: column;
      gap: 16px;
    }
    .feature {
      flex-direction: row;
      gap: 12px;
    }
  }
</style>
