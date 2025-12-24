<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { auth, isAuthenticated, user } from '$lib/auth';
  import { theme } from '$lib/theme';
  import { API_URL } from '$lib/config';

  let code = '';
  let codeInputs: string[] = ['', '', '', '', '', '', '', ''];
  let loading = false;
  let verifying = false;
  let error: string | null = null;
  let deviceName: string | null = null;
  let authorized = false;
  let step: 'enter_code' | 'login' | 'authorize' | 'success' = 'enter_code';

  onMount(async () => {
    // Check if code is in URL
    const urlCode = $page.url.searchParams.get('code');
    if (urlCode) {
      code = urlCode.toUpperCase();
      // Parse into individual inputs
      const cleanCode = code.replace('-', '');
      for (let i = 0; i < 8 && i < cleanCode.length; i++) {
        codeInputs[i] = cleanCode[i];
      }
      await verifyCode();
    }
  });

  function formatCode(inputs: string[]): string {
    const first = inputs.slice(0, 4).join('');
    const second = inputs.slice(4, 8).join('');
    return `${first}-${second}`;
  }

  function handleInput(index: number, event: Event) {
    const input = event.target as HTMLInputElement;
    let value = input.value.toUpperCase();

    // Only allow alphanumeric
    value = value.replace(/[^A-Z0-9]/g, '');

    codeInputs[index] = value.slice(0, 1);
    codeInputs = [...codeInputs];

    // Auto-advance to next input
    if (value && index < 7) {
      const nextInput = document.querySelector(`input[data-index="${index + 1}"]`) as HTMLInputElement;
      if (nextInput) nextInput.focus();
    }

    code = formatCode(codeInputs);
    error = null;
  }

  function handleKeydown(index: number, event: KeyboardEvent) {
    if (event.key === 'Backspace' && !codeInputs[index] && index > 0) {
      const prevInput = document.querySelector(`input[data-index="${index - 1}"]`) as HTMLInputElement;
      if (prevInput) {
        prevInput.focus();
        codeInputs[index - 1] = '';
        codeInputs = [...codeInputs];
      }
    }
  }

  function handlePaste(event: ClipboardEvent) {
    event.preventDefault();
    const pastedText = event.clipboardData?.getData('text') || '';
    const cleanText = pastedText.toUpperCase().replace(/[^A-Z0-9]/g, '');

    for (let i = 0; i < 8 && i < cleanText.length; i++) {
      codeInputs[i] = cleanText[i];
    }
    codeInputs = [...codeInputs];
    code = formatCode(codeInputs);

    // Focus last filled input or first empty
    const lastFilled = Math.min(cleanText.length, 8) - 1;
    const inputToFocus = document.querySelector(`input[data-index="${lastFilled}"]`) as HTMLInputElement;
    if (inputToFocus) inputToFocus.focus();
  }

  async function verifyCode() {
    if (code.length < 9) {
      error = 'Please enter the complete code';
      return;
    }

    verifying = true;
    error = null;

    try {
      const res = await fetch(`${API_URL}/auth/device/verify?code=${encodeURIComponent(code)}`);
      const data = await res.json();

      if (data.success) {
        deviceName = data.data.device_name;

        // If user is already logged in, go to authorize step
        if ($isAuthenticated) {
          step = 'authorize';
        } else {
          step = 'login';
        }
      } else {
        error = data.error === 'Invalid or expired code'
          ? 'This code is invalid or has expired. Please get a new code from your Karoo.'
          : data.error || 'Failed to verify code';
      }
    } catch (err) {
      error = 'Failed to connect. Please check your internet connection.';
    } finally {
      verifying = false;
    }
  }

  function handleLogin() {
    // Store the code in session storage so we can continue after login
    sessionStorage.setItem('device_link_code', code);
    window.location.href = `${API_URL}/auth/login`;
  }

  async function authorizeDevice() {
    if (!$user) {
      error = 'Please sign in first';
      step = 'login';
      return;
    }

    loading = true;
    error = null;

    try {
      const res = await fetch(`${API_URL}/auth/device/authorize`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          user_code: code,
          user_id: $user.id,
        }),
      });

      const data = await res.json();

      if (data.success) {
        step = 'success';
        authorized = true;
      } else {
        error = data.error || 'Failed to authorize device';
      }
    } catch (err) {
      error = 'Failed to connect. Please try again.';
    } finally {
      loading = false;
    }
  }

  // Check if returning from login
  onMount(() => {
    const savedCode = sessionStorage.getItem('device_link_code');
    if (savedCode && $isAuthenticated) {
      sessionStorage.removeItem('device_link_code');
      code = savedCode;

      // Re-verify and authorize
      (async () => {
        const cleanCode = savedCode.replace('-', '');
        for (let i = 0; i < 8 && i < cleanCode.length; i++) {
          codeInputs[i] = cleanCode[i];
        }
        codeInputs = [...codeInputs];

        // Verify first
        const res = await fetch(`${API_URL}/auth/device/verify?code=${encodeURIComponent(savedCode)}`);
        const data = await res.json();
        if (data.success) {
          deviceName = data.data.device_name;
          step = 'authorize';
        }
      })();
    }
  });
</script>

<svelte:head>
  <title>Link Device - KPedal</title>
</svelte:head>

<div class="link-page">
  <div class="link-container">
    <!-- Theme toggle -->
    <button class="theme-toggle" on:click={() => theme.toggle()} aria-label="Toggle theme">
      {#if $theme === 'dark' || ($theme === 'system' && typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches)}
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
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
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
        </svg>
      {/if}
    </button>

    <div class="link-card">
      <!-- Logo -->
      <div class="logo-section">
        <div class="logo">
          <span class="logo-dot"></span>
          <span class="logo-text">KPedal</span>
        </div>
      </div>

      {#if step === 'enter_code'}
        <h1>Link your Karoo</h1>
        <p class="subtitle">Enter the code shown on your Karoo to sync your ride data.</p>

        {#if error}
          <div class="error-banner">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 8v4"/>
              <circle cx="12" cy="16" r="0.5" fill="currentColor"/>
            </svg>
            {error}
          </div>
        {/if}

        <div class="code-inputs" on:paste={handlePaste}>
          {#each codeInputs as char, i}
            {#if i === 4}
              <span class="dash">-</span>
            {/if}
            <input
              type="text"
              maxlength="1"
              data-index={i}
              value={char}
              on:input={(e) => handleInput(i, e)}
              on:keydown={(e) => handleKeydown(i, e)}
              disabled={verifying}
              class:filled={char}
            />
          {/each}
        </div>

        <button class="primary-btn" on:click={verifyCode} disabled={verifying || code.length < 9}>
          {#if verifying}
            <span class="spinner-small"></span>
            Verifying...
          {:else}
            Continue
          {/if}
        </button>

      {:else if step === 'login'}
        <h1>Sign in to continue</h1>
        <p class="subtitle">Sign in with your Google account to link <strong>{deviceName}</strong>.</p>

        {#if error}
          <div class="error-banner">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 8v4"/>
              <circle cx="12" cy="16" r="0.5" fill="currentColor"/>
            </svg>
            {error}
          </div>
        {/if}

        <button class="google-btn" on:click={handleLogin}>
          <svg viewBox="0 0 24 24" width="20" height="20">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
          </svg>
          Continue with Google
        </button>

        <button class="text-btn" on:click={() => step = 'enter_code'}>
          Use a different code
        </button>

      {:else if step === 'authorize'}
        <div class="device-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="5" y="2" width="14" height="20" rx="2" ry="2"/>
            <line x1="12" y1="18" x2="12.01" y2="18"/>
          </svg>
        </div>

        <h1>Authorize {deviceName}?</h1>
        <p class="subtitle">
          Signed in as <strong>{$user?.email}</strong>
          <br>
          This device will be able to sync ride data to your account.
        </p>

        {#if error}
          <div class="error-banner">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 8v4"/>
              <circle cx="12" cy="16" r="0.5" fill="currentColor"/>
            </svg>
            {error}
          </div>
        {/if}

        <div class="button-row">
          <button class="secondary-btn" on:click={() => { step = 'enter_code'; code = ''; codeInputs = ['','','','','','','','']; }}>
            Cancel
          </button>
          <button class="primary-btn" on:click={authorizeDevice} disabled={loading}>
            {#if loading}
              <span class="spinner-small"></span>
              Authorizing...
            {:else}
              Authorize Device
            {/if}
          </button>
        </div>

      {:else if step === 'success'}
        <div class="success-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
        </div>

        <h1>Device Linked!</h1>
        <p class="subtitle">
          <strong>{deviceName}</strong> is now connected to your account.
          <br>
          Your ride data will sync automatically.
        </p>

        <p class="hint">You can close this page and return to your Karoo.</p>

        <button class="primary-btn" on:click={() => goto('/')}>
          Go to Dashboard
        </button>
      {/if}
    </div>

    <p class="footer-text">
      <a href="/privacy">Privacy Policy</a>
    </p>
  </div>
</div>

<style>
  .link-page {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    background: var(--bg-base);
  }

  .link-container {
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

  .link-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 24px;
    padding: 48px 40px;
    box-shadow: var(--shadow-lg);
    text-align: center;
  }

  .logo-section {
    margin-bottom: 32px;
  }

  .logo {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
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

  h1 {
    font-size: 22px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 8px 0;
  }

  .subtitle {
    font-size: 14px;
    color: var(--text-secondary);
    margin: 0 0 24px 0;
    line-height: 1.5;
  }

  .subtitle strong {
    color: var(--text-primary);
  }

  .error-banner {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    padding: 14px 16px;
    background: var(--color-problem-soft);
    color: var(--color-problem);
    border-radius: 12px;
    font-size: 13px;
    margin-bottom: 24px;
    text-align: left;
  }

  .code-inputs {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 6px;
    margin-bottom: 24px;
  }

  .code-inputs input {
    width: 40px;
    height: 52px;
    text-align: center;
    font-size: 20px;
    font-weight: 600;
    font-family: ui-monospace, monospace;
    color: var(--text-primary);
    background: var(--bg-elevated);
    border: 2px solid var(--border-default);
    border-radius: 10px;
    outline: none;
    transition: all 0.2s ease;
  }

  .code-inputs input:focus {
    border-color: var(--color-optimal);
    box-shadow: 0 0 0 3px var(--color-optimal-soft);
  }

  .code-inputs input.filled {
    background: var(--bg-hover);
    border-color: var(--border-strong);
  }

  .code-inputs input:disabled {
    opacity: 0.6;
  }

  .dash {
    font-size: 24px;
    color: var(--text-muted);
    font-weight: 300;
    margin: 0 4px;
  }

  .primary-btn {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 14px 24px;
    background: var(--color-optimal);
    border: none;
    border-radius: 12px;
    font-size: 15px;
    font-weight: 500;
    color: white;
    cursor: pointer;
    transition: all 0.2s ease;
  }

  .primary-btn:hover:not(:disabled) {
    opacity: 0.9;
    transform: translateY(-1px);
  }

  .primary-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .secondary-btn {
    flex: 1;
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

  .secondary-btn:hover {
    background: var(--bg-hover);
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
    margin-bottom: 16px;
  }

  .google-btn:hover {
    background: var(--bg-hover);
    border-color: var(--border-strong);
    transform: translateY(-1px);
    box-shadow: var(--shadow-md);
  }

  .text-btn {
    background: none;
    border: none;
    color: var(--text-secondary);
    font-size: 13px;
    cursor: pointer;
    padding: 8px;
  }

  .text-btn:hover {
    color: var(--text-primary);
  }

  .button-row {
    display: flex;
    gap: 12px;
  }

  .button-row .primary-btn {
    flex: 1;
  }

  .device-icon, .success-icon {
    width: 80px;
    height: 80px;
    margin: 0 auto 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
  }

  .device-icon {
    background: var(--bg-elevated);
    color: var(--text-secondary);
  }

  .success-icon {
    background: var(--color-optimal-soft);
    color: var(--color-optimal);
  }

  .hint {
    font-size: 13px;
    color: var(--text-muted);
    margin: 16px 0 24px;
  }

  .footer-text {
    text-align: center;
    font-size: 12px;
    color: var(--text-muted);
    margin-top: 24px;
  }

  .footer-text a {
    color: var(--text-secondary);
  }

  .spinner-small {
    width: 16px;
    height: 16px;
    border: 2px solid transparent;
    border-top-color: currentColor;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  @media (max-width: 480px) {
    .link-card {
      padding: 32px 24px;
    }

    .code-inputs input {
      width: 36px;
      height: 48px;
      font-size: 18px;
    }
  }
</style>
