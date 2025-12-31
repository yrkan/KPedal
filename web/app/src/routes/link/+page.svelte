<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { auth, isAuthenticated, user } from '$lib/auth';
  import { API_URL } from '$lib/config';
  import { t } from '$lib/i18n';
  import Footer from '$lib/components/Footer.svelte';
  import GuestLogo from '$lib/components/GuestLogo.svelte';
  import GuestControls from '$lib/components/GuestControls.svelte';

  let code = '';
  let codeInputs: string[] = ['', '', '', '', '', '', '', ''];
  let loading = false;
  let verifying = false;
  let error: 'failedToVerify' | 'failedToConnect' | 'enterCompleteCode' | 'invalidOrExpired' | 'checkConnection' | 'signInFirst' | 'failedToAuthorize' | 'failedTryAgain' | null = null;
  let deviceName: string | null = null;
  let authorized = false;
  let step: 'enter_code' | 'login' | 'authorize' | 'success' = 'enter_code';

  onMount(async () => {
    // Check for OAuth callback with tokens (redirect from Google auth)
    const accessToken = $page.url.searchParams.get('access_token');
    const refreshToken = $page.url.searchParams.get('refresh_token');
    const deviceCodeParam = $page.url.searchParams.get('device_code');

    if (accessToken && refreshToken && deviceCodeParam) {
      // OAuth callback - save tokens and auto-authorize
      auth.login(accessToken, refreshToken);

      // Set the device code
      code = deviceCodeParam.toUpperCase();
      const cleanCode = code.replace('-', '');
      for (let i = 0; i < 8 && i < cleanCode.length; i++) {
        codeInputs[i] = cleanCode[i];
      }
      codeInputs = [...codeInputs];

      // Clean URL (remove tokens)
      window.history.replaceState({}, '', window.location.pathname);

      // Verify code and proceed to authorize
      verifying = true;
      try {
        const res = await fetch(`${API_URL}/auth/device/verify?code=${encodeURIComponent(code)}`);
        const data = await res.json();
        if (data.success) {
          deviceName = data.data.device_name;
          step = 'authorize';
          // Auto-authorize since we just logged in
          await authorizeDevice();
        } else {
          error = 'failedToVerify';
          step = 'enter_code';
        }
      } catch {
        error = 'failedToConnect';
        step = 'enter_code';
      } finally {
        verifying = false;
      }
      return;
    }

    // Normal flow - check for code in URL
    const urlCode = $page.url.searchParams.get('code');
    if (urlCode) {
      code = urlCode.toUpperCase();
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
    value = value.replace(/[^A-Z0-9]/g, '');
    codeInputs[index] = value.slice(0, 1);
    codeInputs = [...codeInputs];
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
    const lastFilled = Math.min(cleanText.length, 8) - 1;
    const inputToFocus = document.querySelector(`input[data-index="${lastFilled}"]`) as HTMLInputElement;
    if (inputToFocus) inputToFocus.focus();
  }

  async function verifyCode() {
    if (code.length < 9) {
      error = 'enterCompleteCode';
      return;
    }
    verifying = true;
    error = null;
    try {
      const res = await fetch(`${API_URL}/auth/device/verify?code=${encodeURIComponent(code)}`);
      const data = await res.json();
      if (data.success) {
        deviceName = data.data.device_name;
        if ($isAuthenticated) {
          step = 'authorize';
        } else {
          step = 'login';
        }
      } else {
        error = data.error === 'Invalid or expired code' ? 'invalidOrExpired' : 'failedToVerify';
      }
    } catch (err) {
      error = 'checkConnection';
    } finally {
      verifying = false;
    }
  }

  function handleLogin() {
    // Pass the code in state parameter for cross-origin flow
    const state = encodeURIComponent(JSON.stringify({ device_code: code }));
    window.location.href = `${API_URL}/auth/login?state=${state}`;
  }

  async function authorizeDevice() {
    if (!$user) {
      error = 'signInFirst';
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
        error = 'failedToAuthorize';
      }
    } catch (err) {
      error = 'failedTryAgain';
    } finally {
      loading = false;
    }
  }

</script>

<svelte:head>
  <title>{$t('link.pageTitle')} — KPedal</title>
  <meta name="description" content={$t('link.metaDescription')}>
  <link rel="canonical" href="https://kpedal.com/link">

  <!-- Open Graph -->
  <meta property="og:type" content="website">
  <meta property="og:url" content="https://kpedal.com/link">
  <meta property="og:title" content="{$t('link.pageTitle')} — KPedal">
  <meta property="og:description" content={$t('link.metaDescription')}>

  <!-- Twitter -->
  <meta name="twitter:card" content="summary">
  <meta name="twitter:title" content="{$t('link.pageTitle')} — KPedal">
  <meta name="twitter:description" content={$t('link.metaDescription')}>
</svelte:head>

<div class="link-page">
  <!-- Background decoration -->
  <div class="bg-gradient"></div>
  <div class="bg-pattern"></div>

  <main class="link-main">
    <GuestLogo />

    <div class="link-container">
    <!-- Header actions - only show if not authenticated -->
    {#if !$isAuthenticated}
      <div class="header-actions">
        <GuestControls />
      </div>
    {/if}

    <div class="link-card">
      <!-- Header with security badge -->
      <div class="card-header">
        <div class="security-badge">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            <polyline points="9 12 11 14 15 10"/>
          </svg>
          <span>{$t('link.secureConnection')}</span>
        </div>
      </div>

      {#if step === 'enter_code'}
        <!-- Device illustration -->
        <div class="device-illustration">
          <div class="device-frame">
            <div class="device-screen">
              <div class="device-code-preview">XXXX-0000</div>
            </div>
          </div>
          <div class="connection-line">
            <div class="dot"></div>
            <div class="line"></div>
            <div class="dot"></div>
          </div>
          <div class="cloud-icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z"/>
            </svg>
          </div>
        </div>

        <h1>{$t('link.linkKaroo')}</h1>
        <p class="subtitle">{$t('link.enterCodeDescription')}</p>

        {#if error}
          <div class="error-banner">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            {$t('link.errors.' + error)}
          </div>
        {/if}

        <div class="code-inputs" on:paste={handlePaste}>
          <div class="code-group">
            {#each codeInputs.slice(0, 4) as char, i}
              <input
                type="text"
                maxlength="1"
                data-index={i}
                value={char}
                on:input={(e) => handleInput(i, e)}
                on:keydown={(e) => handleKeydown(i, e)}
                disabled={verifying}
                class:filled={char}
                autocomplete="off"
                autocapitalize="characters"
              />
            {/each}
          </div>
          <span class="dash">-</span>
          <div class="code-group">
            {#each codeInputs.slice(4, 8) as char, i}
              <input
                type="text"
                maxlength="1"
                data-index={i + 4}
                value={char}
                on:input={(e) => handleInput(i + 4, e)}
                on:keydown={(e) => handleKeydown(i + 4, e)}
                disabled={verifying}
                class:filled={char}
                autocomplete="off"
                autocapitalize="characters"
              />
            {/each}
          </div>
        </div>

        <button class="primary-btn" on:click={verifyCode} disabled={verifying || code.length < 9}>
          {#if verifying}
            <span class="spinner"></span>
            {$t('link.verifying')}
          {:else}
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
            {$t('link.continue')}
          {/if}
        </button>

        <!-- Trust indicators -->
        <div class="trust-section">
          <div class="trust-row">
            <div class="trust-item">
              <div class="trust-icon">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                </svg>
              </div>
              <span>{$t('link.encryption256')}</span>
            </div>
            <div class="trust-item">
              <div class="trust-icon check">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                </svg>
              </div>
              <span>{$t('link.verifiedDomain')}</span>
            </div>
          </div>
          <div class="trust-url">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
            <span>https://link.kpedal.com</span>
          </div>
        </div>

      {:else if step === 'login'}
        <div class="step-icon">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
            <circle cx="12" cy="7" r="4"/>
          </svg>
        </div>

        <h1>{$t('link.signInToContinue')}</h1>
        <p class="subtitle">
          {$t('link.linkingDevice', { values: { device: deviceName || 'Karoo' } })}<br/>
          {$t('link.signInWithGoogle')}
        </p>

        {#if error}
          <div class="error-banner">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            {$t('link.errors.' + error)}
          </div>
        {/if}

        <button class="google-btn" on:click={handleLogin}>
          <svg viewBox="0 0 24 24" width="18" height="18">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
          </svg>
          {$t('auth.continueWithGoogle')}
        </button>

        <button class="text-btn" on:click={() => { step = 'enter_code'; code = ''; codeInputs = ['','','','','','','','']; error = null; }}>
          {$t('link.useDifferentCode')}
        </button>

        <div class="trust-section compact">
          <div class="trust-row">
            <div class="trust-item">
              <div class="trust-icon">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                </svg>
              </div>
              <span>{$t('auth.secureOAuth')}</span>
            </div>
            <div class="trust-item">
              <div class="trust-icon check">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
              </div>
              <span>{$t('auth.googleVerified')}</span>
            </div>
          </div>
        </div>

      {:else if step === 'authorize'}
        <div class="step-icon authorize">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            <polyline points="9 12 11 14 15 10"/>
          </svg>
        </div>

        <h1>{$t('link.authorizeDevice')}</h1>
        <p class="subtitle">
          {$t('link.deviceRequestingAccess', { values: { device: deviceName || 'Karoo' } })}
        </p>

        <div class="account-info">
          <div class="account-avatar">
            {#if $user?.picture}
              <img src={$user.picture} alt="" />
            {:else}
              <span>{$user?.name?.charAt(0) || '?'}</span>
            {/if}
          </div>
          <div class="account-details">
            <div class="account-name">{$user?.name}</div>
            <div class="account-email">{$user?.email}</div>
          </div>
        </div>

        {#if error}
          <div class="error-banner">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            {$t('link.errors.' + error)}
          </div>
        {/if}

        <div class="permissions-list">
          <div class="permission-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            <span>{$t('link.permissionSync')}</span>
          </div>
          <div class="permission-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            <span>{$t('link.permissionView')}</span>
          </div>
        </div>

        <div class="button-row">
          <button class="secondary-btn" on:click={() => { step = 'enter_code'; code = ''; codeInputs = ['','','','','','','','']; }}>
            {$t('common.cancel')}
          </button>
          <button class="primary-btn" on:click={authorizeDevice} disabled={loading}>
            {#if loading}
              <span class="spinner"></span>
            {:else}
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
              {$t('link.authorize')}
            {/if}
          </button>
        </div>

      {:else if step === 'success'}
        <div class="success-animation">
          <div class="success-circle">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
          </div>
        </div>

        <h1>{$t('link.deviceLinked')}</h1>
        <p class="subtitle">
          {$t('link.deviceConnected', { values: { device: deviceName } })}<br/>
          {$t('link.autoSync')}
        </p>

        <div class="success-info">
          <div class="success-item">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
            <div>
              <strong>{$t('link.secureConnectionEstablished')}</strong>
              <span>{$t('link.dataEncrypted')}</span>
            </div>
          </div>
        </div>

        <p class="hint">{$t('link.closePageHint')}</p>

        <button class="primary-btn" on:click={() => goto('/')}>
          {$t('link.goToDashboard')}
        </button>
      {/if}
    </div>

  </div>
  </main>

  <Footer />
</div>

<style>
  .link-page {
    min-height: 100dvh;
    display: flex;
    flex-direction: column;
    background: var(--bg-base);
    position: relative;
  }

  .bg-gradient {
    position: fixed;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle at 30% 20%, var(--color-optimal-soft) 0%, transparent 40%),
                radial-gradient(circle at 70% 80%, var(--color-optimal-soft) 0%, transparent 30%);
    opacity: 0.5;
    pointer-events: none;
    z-index: 0;
  }

  .bg-pattern {
    position: fixed;
    inset: 0;
    background-image: radial-gradient(var(--border-subtle) 1px, transparent 1px);
    background-size: 24px 24px;
    z-index: 0;
    opacity: 0.5;
    pointer-events: none;
  }

  .link-main {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
    position: relative;
    z-index: 1;
  }

  .link-container {
    width: 100%;
    max-width: 420px;
    position: relative;
    z-index: 1;
  }

  .header-actions {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 12px;
  }

  .link-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 24px;
    padding: 32px;
    text-align: center;
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 24px;
  }

  .security-badge {
    display: flex;
    align-items: center;
    gap: 5px;
    padding: 6px 10px;
    background: var(--color-optimal-soft);
    color: var(--color-optimal-text);
    border-radius: 20px;
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .device-illustration {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    margin: 20px 0 28px;
  }

  .device-frame {
    width: 64px;
    height: 80px;
    background: var(--bg-elevated);
    border: 2px solid var(--border-default);
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 4px;
  }

  .device-screen {
    width: 100%;
    height: 100%;
    background: var(--bg-base);
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .device-code-preview {
    font-size: 8px;
    font-weight: 600;
    color: var(--text-muted);
    font-family: ui-monospace, monospace;
  }

  .connection-line {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .connection-line .dot {
    width: 6px;
    height: 6px;
    background: var(--color-optimal);
    border-radius: 50%;
    animation: pulse 1.5s ease-in-out infinite;
  }

  .connection-line .line {
    width: 24px;
    height: 2px;
    background: linear-gradient(90deg, var(--color-optimal), var(--color-optimal-soft));
    border-radius: 1px;
  }

  .cloud-icon {
    width: 48px;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--color-optimal);
    background: var(--color-optimal-soft);
    border-radius: 12px;
  }

  @keyframes pulse {
    0%, 100% { opacity: 1; transform: scale(1); }
    50% { opacity: 0.5; transform: scale(0.8); }
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
    font-weight: 500;
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
    line-height: 1.4;
  }

  .code-inputs {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: clamp(6px, 2vw, 10px);
    margin-bottom: 24px;
    padding: 0 8px;
  }

  .code-group {
    display: flex;
    gap: clamp(4px, 1.2vw, 6px);
  }

  .code-inputs input {
    width: clamp(30px, 7.5vw, 36px);
    height: clamp(38px, 9.5vw, 46px);
    text-align: center;
    font-size: clamp(15px, 3.8vw, 18px);
    font-weight: 600;
    font-family: ui-monospace, 'SF Mono', monospace;
    color: var(--text-primary);
    background: var(--bg-elevated);
    border: 2px solid rgba(255, 255, 255, 0.2);
    border-radius: clamp(6px, 1.5vw, 8px);
    outline: none;
    transition: all 0.15s ease;
    -webkit-appearance: none;
  }

  .dash {
    font-size: clamp(18px, 5vw, 24px);
    color: var(--text-muted);
    font-weight: 300;
  }

  :global([data-theme="light"]) .code-inputs input {
    border-color: rgba(0, 0, 0, 0.2);
  }

  .code-inputs input:focus {
    border-color: var(--color-optimal);
    background: var(--bg-surface);
    box-shadow: 0 0 0 3px var(--color-optimal-soft);
  }

  .code-inputs input.filled {
    border-color: var(--color-optimal);
    background: var(--color-optimal-soft);
  }

  .code-inputs input:disabled {
    opacity: 0.5;
  }

  .dash {
    font-size: 24px;
    color: var(--text-muted);
    font-weight: 300;
  }

  .step-icon {
    width: 72px;
    height: 72px;
    margin: 0 auto 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: var(--bg-elevated);
    color: var(--text-secondary);
  }

  .step-icon.authorize {
    background: var(--color-attention-soft, rgba(255, 193, 7, 0.15));
    color: var(--color-attention);
  }

  .account-info {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 16px;
    background: var(--bg-elevated);
    border-radius: 12px;
    margin-bottom: 16px;
  }

  .account-avatar {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: var(--color-optimal);
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    flex-shrink: 0;
    border: 2px solid var(--border-default);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  .account-avatar img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .account-avatar span {
    color: var(--color-accent-text);
    font-weight: 600;
    font-size: 16px;
  }

  .account-details {
    text-align: left;
  }

  .account-name {
    font-weight: 500;
    color: var(--text-primary);
    font-size: 14px;
  }

  .account-email {
    color: var(--text-secondary);
    font-size: 12px;
  }

  .permissions-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-bottom: 20px;
    text-align: left;
  }

  .permission-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    background: var(--bg-base);
    border-radius: 8px;
    font-size: 13px;
    color: var(--text-secondary);
  }

  .permission-item svg {
    color: var(--color-optimal);
    flex-shrink: 0;
  }

  .primary-btn {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 14px 24px;
    background: var(--color-accent);
    border: none;
    border-radius: 12px;
    font-size: 15px;
    font-weight: 500;
    color: var(--color-accent-text);
    cursor: pointer;
    transition: all 0.15s ease;
  }

  .primary-btn:hover:not(:disabled) {
    background: var(--color-accent-hover);
    transform: translateY(-1px);
    box-shadow: var(--shadow-md), var(--glow-accent);
  }

  .primary-btn:active:not(:disabled) {
    transform: translateY(0);
  }

  .primary-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .secondary-btn {
    flex: 1;
    padding: 14px 20px;
    background: var(--bg-elevated);
    border: 1px solid var(--border-default);
    border-radius: 12px;
    font-size: 15px;
    font-weight: 500;
    color: var(--text-primary);
    cursor: pointer;
    transition: background 0.15s ease;
  }

  .secondary-btn:hover {
    background: var(--bg-hover);
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
    margin-bottom: 12px;
  }

  .google-btn:hover {
    background: var(--bg-hover);
    border-color: var(--border-strong);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  .text-btn {
    background: none;
    border: none;
    color: var(--text-secondary);
    font-size: 13px;
    cursor: pointer;
    padding: 8px 12px;
    border-radius: 6px;
    transition: color 0.15s ease;
  }

  .text-btn:hover {
    color: var(--text-primary);
  }

  .button-row {
    display: flex;
    gap: 10px;
  }

  .button-row .primary-btn {
    flex: 1.2;
  }

  .trust-section {
    margin-top: 24px;
    padding-top: 20px;
    border-top: 1px solid var(--border-subtle);
  }

  .trust-section.compact {
    margin-top: 16px;
    padding-top: 16px;
  }

  .trust-row {
    display: flex;
    justify-content: center;
    gap: 16px;
    flex-wrap: wrap;
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

  .trust-url {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    margin-top: 12px;
    padding: 8px 12px;
    background: var(--bg-base);
    border-radius: 8px;
    font-size: 12px;
    font-family: ui-monospace, monospace;
    color: var(--color-optimal-text);
  }

  .trust-url svg {
    color: var(--color-optimal);
  }

  .success-animation {
    margin-bottom: 24px;
  }

  .success-circle {
    width: 88px;
    height: 88px;
    margin: 0 auto;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: var(--color-optimal-soft);
    color: var(--color-optimal);
    animation: scaleIn 0.4s ease-out;
  }

  @keyframes scaleIn {
    0% { transform: scale(0); opacity: 0; }
    50% { transform: scale(1.1); }
    100% { transform: scale(1); opacity: 1; }
  }

  .success-info {
    margin: 20px 0;
    padding: 16px;
    background: var(--bg-elevated);
    border-radius: 12px;
  }

  .success-item {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    text-align: left;
  }

  .success-item svg {
    color: var(--color-optimal);
    flex-shrink: 0;
    margin-top: 2px;
  }

  .success-item strong {
    display: block;
    color: var(--text-primary);
    font-size: 14px;
    margin-bottom: 2px;
  }

  .success-item span {
    color: var(--text-secondary);
    font-size: 12px;
  }

  .hint {
    font-size: 13px;
    color: var(--text-muted);
    margin: 20px 0;
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

  /* Mobile */
  @media (max-width: 420px) {
    .link-page {
      padding: 16px;
    }

    .link-card {
      padding: 24px 20px;
      border-radius: 20px;
    }

    .code-inputs input {
      width: 34px;
      height: 44px;
      font-size: 18px;
      border-radius: 8px;
    }

    .code-group {
      gap: 4px;
    }

    .code-inputs {
      gap: 6px;
    }

    h1 {
      font-size: 20px;
    }

    .device-illustration {
      transform: scale(0.9);
    }

    .security-badge span {
      display: none;
    }

    .security-badge {
      padding: 8px;
    }
  }

  /* 2-row layout for small screens */
  @media (max-width: 420px) {
    .code-inputs {
      flex-wrap: wrap;
      gap: 0;
    }

    .code-group {
      gap: clamp(5px, 2vw, 8px);
    }

    .code-inputs input {
      width: clamp(36px, 15vw, 44px);
      height: clamp(42px, 16vw, 50px);
      font-size: clamp(16px, 4.5vw, 20px);
    }

    .dash {
      width: 100%;
      text-align: center;
      font-size: 16px;
      margin: 4px 0;
    }
  }

  /* Card shadow uses theme-aware shadows */
  .link-card {
    box-shadow: var(--shadow-lg);
  }

  /* Filled inputs use theme variables */
  .code-inputs input.filled {
    border-color: var(--color-accent);
    background: var(--color-optimal-soft);
  }
</style>
