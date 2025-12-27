<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { auth } from '$lib/auth';
  import { t } from '$lib/i18n';

  let error: string | null = null;

  // Redirect to app.kpedal.com if on kpedal.com (landing domain)
  function redirectToDashboard() {
    if (typeof window !== 'undefined' && window.location.hostname === 'kpedal.com') {
      window.location.href = 'https://app.kpedal.com/';
    } else {
      goto('/');
    }
  }

  onMount(() => {
    const accessToken = $page.url.searchParams.get('access_token');
    const refreshToken = $page.url.searchParams.get('refresh_token');

    if (!accessToken || !refreshToken) {
      error = $t('auth.missingTokens');
      setTimeout(() => goto('/login?error=auth_failed'), 2000);
      return;
    }

    // Store tokens
    auth.login(accessToken, refreshToken);

    // Clear tokens from URL (security)
    window.history.replaceState({}, '', '/');

    // Check if user was linking a device - redirect back to /link to complete authorization
    const deviceLinkCode = sessionStorage.getItem('device_link_code');
    if (deviceLinkCode) {
      goto('/link');
    } else {
      redirectToDashboard();
    }
  });
</script>

<svelte:head>
  <title>{$t('auth.authenticating')} - KPedal</title>
</svelte:head>

<div class="auth-page">
  <div class="auth-card">
    {#if error}
      <div class="error">
        <div class="error-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/>
            <path d="M12 8v4"/>
            <circle cx="12" cy="16" r="0.5" fill="currentColor"/>
          </svg>
        </div>
        <p class="error-message">{error}</p>
        <p class="redirect">{$t('auth.redirectingToLogin')}</p>
      </div>
    {:else}
      <div class="loading">
        <div class="spinner"></div>
        <p>{$t('auth.signingIn')}</p>
      </div>
    {/if}
  </div>
</div>

<style>
  .auth-page {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
    background: var(--bg-base);
  }

  .auth-card {
    background: var(--bg-surface);
    border-radius: 16px;
    padding: 48px;
    text-align: center;
    border: 1px solid var(--border-subtle);
    min-width: 280px;
  }

  .loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
  }

  .loading p {
    color: var(--text-secondary);
    font-size: 15px;
  }

  .error {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
  }

  .error-icon {
    width: 48px;
    height: 48px;
    background: var(--color-problem-soft);
    color: var(--color-problem);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .error-message {
    color: var(--color-problem);
    font-size: 15px;
    font-weight: 500;
  }

  .redirect {
    color: var(--text-muted);
    font-size: 12px;
    margin-top: 4px;
  }
</style>
