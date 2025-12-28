<script lang="ts">
  import { onMount } from 'svelte';
  import { browser } from '$app/environment';
  import { theme, resolvedTheme } from '$lib/theme';
  import { t, locale, locales, localeNames, setLocale, type Locale } from '$lib/i18n';
  import Footer from '$lib/components/Footer.svelte';

  // State
  let completedSteps: Set<number> = new Set();
  let expandedStep: number | null = null;
  let checkedItems: Set<string> = new Set();
  let mounted = false;

  const STORAGE_KEY = 'kpedal-setup-progress';
  const TOTAL_STEPS = 4;

  const stepIds = ['install', 'fields', 'configure', 'sync'] as const;
  const optionalSteps = new Set(['configure', 'sync']);

  // Load progress from localStorage
  function loadProgress() {
    if (!browser) return;
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        const data = JSON.parse(saved);
        completedSteps = new Set(data.completedSteps || []);
        checkedItems = new Set(data.checkedItems || []);
      }
    } catch {}
  }

  // Save progress to localStorage
  function saveProgress() {
    if (!browser) return;
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        completedSteps: [...completedSteps],
        checkedItems: [...checkedItems],
      }));
    } catch {}
  }

  function toggleCheck(id: string) {
    if (checkedItems.has(id)) {
      checkedItems.delete(id);
    } else {
      checkedItems.add(id);
    }
    checkedItems = checkedItems;
    saveProgress();
  }

  function markComplete(stepIndex: number) {
    completedSteps.add(stepIndex);
    completedSteps = completedSteps;
    saveProgress();

    // Auto-expand next step
    if (stepIndex < TOTAL_STEPS - 1) {
      expandedStep = stepIndex + 1;
      // Smooth scroll to next step
      setTimeout(() => {
        const nextCard = document.querySelector(`.step-card:nth-child(${stepIndex + 2})`);
        nextCard?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }, 100);
    }
  }

  function toggleStep(index: number) {
    expandedStep = expandedStep === index ? null : index;
  }

  function resetProgress() {
    completedSteps = new Set();
    checkedItems = new Set();
    expandedStep = 0;
    if (browser) {
      localStorage.removeItem(STORAGE_KEY);
    }
  }

  onMount(() => {
    loadProgress();
    // Expand first incomplete step
    for (let i = 0; i < TOTAL_STEPS; i++) {
      if (!completedSteps.has(i)) {
        expandedStep = i;
        break;
      }
    }
    if (completedSteps.size === TOTAL_STEPS) {
      expandedStep = TOTAL_STEPS - 1;
    }
    mounted = true;
  });
</script>

<svelte:head>
  <title>{$t('start.meta.title')}</title>
  <meta name="description" content={$t('start.meta.description')}>
  <link rel="canonical" href="https://start.kpedal.com">
</svelte:head>

<div class="start-page" class:mounted>
  <!-- Background decorations -->
  <div class="bg-decorations">
    <div class="bg-blob blob-1"></div>
    <div class="bg-blob blob-2"></div>
    <div class="bg-grid"></div>
  </div>

  <!-- Header -->
  <header class="header">
    <a href="https://kpedal.com" class="logo">
      <span class="logo-dot"></span>
      <span class="logo-text">KPedal</span>
    </a>

    <div class="header-actions">
      <select
        class="lang-select"
        value={$locale}
        on:change={(e) => setLocale(e.currentTarget.value as Locale)}
        aria-label="Select language"
      >
        {#each locales as loc}
          <option value={loc}>{localeNames[loc]}</option>
        {/each}
      </select>

      <button class="theme-toggle" on:click={() => theme.toggle()} aria-label="Toggle theme">
        {#if $resolvedTheme === 'light'}
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="5"/>
            <line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
            <line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
          </svg>
        {:else}
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
          </svg>
        {/if}
        {#if $theme === 'auto'}
          <span class="auto-badge">A</span>
        {/if}
      </button>
    </div>
  </header>

  <main class="main">
    <!-- Hero -->
    <div class="hero">
      <h1>{$t('start.title')}</h1>
      <p class="hero-subtitle">{$t('start.subtitle')}</p>

      <!-- Progress -->
      <div class="progress-container">
        <div class="progress-bar">
          <div class="progress-fill" style="width: {(completedSteps.size / TOTAL_STEPS) * 100}%"></div>
        </div>
        <div class="progress-info">
          <span class="progress-text">{$t('start.progress', { values: { completed: completedSteps.size, total: TOTAL_STEPS } })}</span>
          {#if completedSteps.size > 0 && completedSteps.size < TOTAL_STEPS}
            <button class="reset-btn" on:click={resetProgress}>{$t('start.reset')}</button>
          {/if}
        </div>
      </div>
    </div>

    <!-- Steps Timeline -->
    <div class="steps-timeline">
      {#each stepIds as stepId, index}
        <div
          class="step-card"
          class:active={expandedStep === index}
          class:completed={completedSteps.has(index)}
          style="--delay: {index * 0.1}s"
        >
          <!-- Step Header -->
          <button class="step-header" on:click={() => toggleStep(index)}>
            <div class="step-indicator" class:completed={completedSteps.has(index)}>
              {#if completedSteps.has(index)}
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
              {:else}
                <!-- Step icons -->
                {#if stepId === 'install'}
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="7 10 12 15 17 10"/>
                    <line x1="12" y1="15" x2="12" y2="3"/>
                  </svg>
                {:else if stepId === 'fields'}
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="3" width="18" height="18" rx="2"/>
                    <path d="M3 9h18"/>
                    <path d="M9 21V9"/>
                  </svg>
                {:else if stepId === 'configure'}
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="3"/>
                    <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
                  </svg>
                {:else}
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z"/>
                  </svg>
                {/if}
              {/if}
            </div>

            <div class="step-info">
              <div class="step-title-row">
                <h3>{$t(`start.steps.${stepId}.title`)}</h3>
                {#if optionalSteps.has(stepId)}
                  <span class="optional-badge">{$t('start.optional')}</span>
                {/if}
              </div>
              <span class="step-time">{$t(`start.steps.${stepId}.time`)}</span>
            </div>

            <div class="step-toggle" class:expanded={expandedStep === index}>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9"/>
              </svg>
            </div>
          </button>

          <!-- Step Content -->
          {#if expandedStep === index}
            <div class="step-content">
              {#if stepId === 'install'}
                <!-- Install Step -->
                <p class="intro">{@html $t('start.steps.install.intro')}</p>

                <div class="checklist">
                  <label class="check-item" class:checked={checkedItems.has('companion')}>
                    <input type="checkbox" checked={checkedItems.has('companion')} on:change={() => toggleCheck('companion')}>
                    <span class="checkmark"></span>
                    <span>{$t('start.steps.install.hasCompanion')}</span>
                  </label>
                </div>

                <div class="action-box">
                  <a href="https://github.com/yrkan/KPedal/releases/latest" target="_blank" rel="noopener noreferrer" class="action-btn">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                      <polyline points="7 10 12 15 17 10"/>
                      <line x1="12" y1="15" x2="12" y2="3"/>
                    </svg>
                    {$t('start.steps.install.downloadApk')}
                  </a>
                  <p class="action-hint">{$t('start.steps.install.opensGithub')}</p>
                </div>

                <div class="numbered-steps">
                  <div class="numbered-step"><span class="num">1</span><span>{@html $t('start.steps.install.step1')}</span></div>
                  <div class="numbered-step"><span class="num">2</span><span>{@html $t('start.steps.install.step2')}</span></div>
                  <div class="numbered-step"><span class="num">3</span><span>{@html $t('start.steps.install.step3')}</span></div>
                  <div class="numbered-step"><span class="num">4</span><span>{@html $t('start.steps.install.step4')}</span></div>
                </div>

                <div class="checklist">
                  <label class="check-item" class:checked={checkedItems.has('installed')}>
                    <input type="checkbox" checked={checkedItems.has('installed')} on:change={() => toggleCheck('installed')}>
                    <span class="checkmark"></span>
                    <span>{$t('start.steps.install.installed')}</span>
                  </label>
                </div>

                <button class="complete-btn" on:click={() => markComplete(index)} disabled={!checkedItems.has('installed')}>
                  {$t('start.continue')}
                </button>

              {:else if stepId === 'fields'}
                <!-- Data Fields Step -->
                <p class="intro">{$t('start.steps.fields.intro')}</p>

                <div class="numbered-steps">
                  <div class="numbered-step"><span class="num">1</span><span>{@html $t('start.steps.fields.step1')}</span></div>
                  <div class="numbered-step"><span class="num">2</span><span>{@html $t('start.steps.fields.step2')}</span></div>
                  <div class="numbered-step"><span class="num">3</span><span>{@html $t('start.steps.fields.step3')}</span></div>
                  <div class="numbered-step"><span class="num">4</span><span>{@html $t('start.steps.fields.step4')}</span></div>
                </div>

                <details class="expandable">
                  <summary>{$t('start.steps.fields.allFields')}</summary>
                  <ul class="field-list">
                    <li>{@html $t('start.steps.fields.fieldQuickGlance')}</li>
                    <li>{@html $t('start.steps.fields.fieldPowerBalance')}</li>
                    <li>{@html $t('start.steps.fields.fieldEfficiency')}</li>
                    <li>{@html $t('start.steps.fields.fieldFullOverview')}</li>
                    <li>{@html $t('start.steps.fields.fieldBalanceTrend')}</li>
                  </ul>
                </details>

                <button class="complete-btn" on:click={() => markComplete(index)}>
                  {$t('start.done')}
                </button>

              {:else if stepId === 'configure'}
                <!-- Configure Step -->
                <p class="intro">{$t('start.steps.configure.intro')}</p>

                <div class="numbered-steps">
                  <div class="numbered-step"><span class="num">1</span><span>{@html $t('start.steps.configure.step1')}</span></div>
                </div>

                <details class="expandable">
                  <summary>{$t('start.steps.configure.whatYouCanAdjust')}</summary>
                  <ul class="field-list">
                    <li>{@html $t('start.steps.configure.optionBalance')}</li>
                    <li>{@html $t('start.steps.configure.optionAlerts')}</li>
                    <li>{@html $t('start.steps.configure.optionDrills')}</li>
                  </ul>
                </details>

                <div class="button-row">
                  <button class="complete-btn secondary" on:click={() => markComplete(index)}>{$t('start.skip')}</button>
                  <button class="complete-btn" on:click={() => markComplete(index)}>{$t('start.done')}</button>
                </div>

              {:else if stepId === 'sync'}
                <!-- Cloud Sync Step -->
                <p class="intro">{$t('start.steps.sync.intro')}</p>

                <div class="numbered-steps">
                  <div class="numbered-step"><span class="num">1</span><span>{@html $t('start.steps.sync.step1')}</span></div>
                  <div class="numbered-step"><span class="num">2</span><span>{$t('start.steps.sync.step2')}</span></div>
                  <div class="numbered-step"><span class="num">3</span><span>{$t('start.steps.sync.step3')} <a href="https://link.kpedal.com" target="_blank" rel="noopener noreferrer" class="link-highlight">link.kpedal.com</a></span></div>
                  <div class="numbered-step"><span class="num">4</span><span>{$t('start.steps.sync.step4')}</span></div>
                </div>

                <p class="note">{$t('start.steps.sync.note')}</p>

                <div class="button-row">
                  <button class="complete-btn secondary" on:click={() => markComplete(index)}>{$t('start.skip')}</button>
                  <button class="complete-btn" on:click={() => markComplete(index)}>{$t('start.done')}</button>
                </div>
              {/if}
            </div>
          {/if}
        </div>

        <!-- Connector line between steps -->
        {#if index < TOTAL_STEPS - 1}
          <div class="step-connector" class:completed={completedSteps.has(index)}></div>
        {/if}
      {/each}
    </div>

    <!-- Completion -->
    {#if completedSteps.size === TOTAL_STEPS}
      <div class="completion-card">
        <div class="completion-confetti">
          <span></span><span></span><span></span><span></span><span></span>
        </div>
        <div class="completion-icon">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="20 6 9 17 4 12"/>
          </svg>
        </div>
        <h2>{$t('start.completion.title')}</h2>
        <p>{$t('start.completion.subtitle')}</p>

        <!-- App Preview -->
        <div class="app-preview">
          <div class="preview-header">
            <span class="preview-dot"></span>
            <span class="preview-title">Quick Glance</span>
          </div>
          <div class="preview-metrics">
            <div class="preview-metric optimal">
              <span class="metric-value">48</span>
              <span class="metric-label">L</span>
            </div>
            <div class="preview-divider"></div>
            <div class="preview-metric optimal">
              <span class="metric-value">52</span>
              <span class="metric-label">R</span>
            </div>
          </div>
          <div class="preview-bar">
            <div class="bar-left" style="width: 48%"></div>
            <div class="bar-right" style="width: 52%"></div>
          </div>
        </div>

        <div class="color-hint">
          <span><span class="color-dot optimal"></span> {$t('start.completion.optimal')}</span>
          <span><span class="color-dot attention"></span> {$t('start.completion.attention')}</span>
          <span><span class="color-dot problem"></span> {$t('start.completion.problem')}</span>
        </div>

        <div class="completion-links">
          <a href="https://app.kpedal.com/login" class="completion-link primary">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <path d="M3 9h18"/>
              <path d="M9 21V9"/>
            </svg>
            {$t('start.completion.webDashboard')}
          </a>
          <a href="https://github.com/yrkan/KPedal" target="_blank" rel="noopener noreferrer" class="completion-link secondary">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/>
            </svg>
            {$t('start.footer.github')}
          </a>
        </div>
      </div>
    {/if}

  </main>

  <Footer />
</div>

<style>
  .start-page {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    background: var(--bg-base);
    position: relative;
    overflow-x: hidden;
  }

  /* Background decorations */
  .bg-decorations {
    position: fixed;
    inset: 0;
    pointer-events: none;
    z-index: 0;
    overflow: hidden;
  }

  .bg-blob {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    opacity: 0.5;
  }

  .blob-1 {
    width: 500px;
    height: 500px;
    background: var(--color-optimal);
    top: -200px;
    left: -100px;
    opacity: 0.15;
  }

  .blob-2 {
    width: 400px;
    height: 400px;
    background: var(--color-accent, var(--color-optimal));
    bottom: -100px;
    right: -100px;
    opacity: 0.1;
  }

  .bg-grid {
    position: absolute;
    inset: 0;
    background-image:
      linear-gradient(var(--border-subtle) 1px, transparent 1px),
      linear-gradient(90deg, var(--border-subtle) 1px, transparent 1px);
    background-size: 40px 40px;
    opacity: 0.3;
    mask-image: radial-gradient(ellipse at center, black 0%, transparent 70%);
  }

  main {
    flex: 1;
    position: relative;
    z-index: 1;
  }

  /* Header */
  .header {
    position: sticky;
    top: 0;
    z-index: 100;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 24px;
    background: var(--bg-base);
    backdrop-filter: blur(8px);
    border-bottom: 1px solid var(--border-subtle);
  }

  .logo {
    display: flex;
    align-items: center;
    gap: 10px;
    text-decoration: none;
  }

  .logo-dot {
    width: 10px;
    height: 10px;
    background: var(--color-optimal);
    border-radius: 50%;
    animation: pulse-dot 2s ease-in-out infinite;
  }

  @keyframes pulse-dot {
    0%, 100% { transform: scale(1); opacity: 1; }
    50% { transform: scale(1.2); opacity: 0.8; }
  }

  .logo-text {
    font-size: 18px;
    font-weight: 700;
    color: var(--text-primary);
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .lang-select {
    appearance: none;
    height: 36px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 8px;
    padding: 0 28px 0 12px;
    font-size: 13px;
    color: var(--text-secondary);
    cursor: pointer;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%236b7280' stroke-width='2'%3E%3Cpolyline points='6,9 12,15 18,9'/%3E%3C/svg%3E");
    background-repeat: no-repeat;
    background-position: right 8px center;
    transition: border-color 0.15s, box-shadow 0.15s;
  }

  .lang-select:hover {
    border-color: var(--color-optimal);
  }

  .theme-toggle {
    position: relative;
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    color: var(--text-secondary);
    cursor: pointer;
    transition: all 0.15s;
  }

  .theme-toggle:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
    border-color: var(--color-optimal);
  }

  .auto-badge {
    position: absolute;
    bottom: -4px;
    right: -4px;
    width: 14px;
    height: 14px;
    background: #22c55e;
    color: #fff;
    font-size: 9px;
    font-weight: 900;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 2px solid var(--bg-base);
  }

  /* Main */
  .main {
    max-width: 600px;
    margin: 0 auto;
    padding: 40px 20px 60px;
    position: relative;
    z-index: 1;
  }

  /* Hero */
  .hero {
    text-align: center;
    margin-bottom: 40px;
  }

  .hero h1 {
    font-size: 32px;
    font-weight: 700;
    color: var(--text-primary);
    margin-bottom: 8px;
    letter-spacing: -0.5px;
  }

  .hero-subtitle {
    font-size: 16px;
    color: var(--text-secondary);
    margin-bottom: 20px;
  }

  .progress-container {
    max-width: 320px;
    margin: 0 auto;
  }

  .progress-bar {
    height: 8px;
    background: var(--bg-elevated);
    border-radius: 4px;
    overflow: hidden;
    box-shadow: inset 0 1px 2px rgba(0,0,0,0.1);
  }

  .progress-fill {
    height: 100%;
    background: linear-gradient(90deg, var(--color-optimal), #10b981);
    border-radius: 4px;
    transition: width 0.4s ease;
    position: relative;
  }

  .progress-fill::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(90deg, transparent 0%, rgba(255,255,255,0.3) 50%, transparent 100%);
    animation: shimmer 2s infinite;
  }

  @keyframes shimmer {
    0% { transform: translateX(-100%); }
    100% { transform: translateX(100%); }
  }

  .progress-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 10px;
    font-size: 13px;
  }

  .progress-text {
    color: var(--text-muted);
  }

  .reset-btn {
    background: none;
    border: none;
    color: var(--text-muted);
    font-size: 12px;
    cursor: pointer;
    text-decoration: underline;
    transition: color 0.15s;
  }

  .reset-btn:hover {
    color: var(--color-problem);
  }

  /* Steps Timeline */
  .steps-timeline {
    display: flex;
    flex-direction: column;
    position: relative;
  }

  .step-connector {
    width: 2px;
    height: 20px;
    background: var(--border-subtle);
    margin-left: 31px;
    transition: background 0.3s;
  }

  .step-connector.completed {
    background: var(--color-optimal);
  }

  .step-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: 16px;
    overflow: hidden;
    transition: all 0.25s ease;
    opacity: 0;
    transform: translateY(10px);
    animation: slideIn 0.4s ease forwards;
    animation-delay: var(--delay, 0s);
  }

  .mounted .step-card {
    opacity: 1;
    transform: translateY(0);
  }

  @keyframes slideIn {
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  .step-card:hover {
    border-color: var(--border-default);
    box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  }

  .step-card.active {
    border-color: var(--color-optimal);
    box-shadow: 0 0 0 3px var(--color-optimal-soft);
  }

  .step-card.completed {
    opacity: 0.7;
  }

  .step-card.completed:hover {
    opacity: 1;
  }

  .step-header {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 18px;
    background: none;
    border: none;
    cursor: pointer;
    text-align: left;
    color: inherit;
  }

  .step-indicator {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-elevated);
    border-radius: 10px;
    color: var(--text-muted);
    flex-shrink: 0;
    transition: all 0.2s;
  }

  .step-card:hover .step-indicator {
    background: var(--color-optimal-soft);
    color: var(--color-optimal);
  }

  .step-indicator.completed {
    background: var(--color-optimal);
    color: white;
  }

  .step-info {
    flex: 1;
    min-width: 0;
  }

  .step-title-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .step-info h3 {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary);
  }

  .optional-badge {
    font-size: 10px;
    color: var(--text-muted);
    background: var(--bg-elevated);
    padding: 3px 8px;
    border-radius: 10px;
    font-weight: 500;
  }

  .step-time {
    font-size: 13px;
    color: var(--text-muted);
  }

  .step-toggle {
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-muted);
    transition: transform 0.25s ease;
  }

  .step-toggle.expanded {
    transform: rotate(180deg);
  }

  /* Step Content */
  .step-content {
    padding: 0 18px 20px;
    border-top: 1px solid var(--border-subtle);
    animation: fadeSlide 0.25s ease;
  }

  @keyframes fadeSlide {
    from { opacity: 0; transform: translateY(-8px); }
    to { opacity: 1; transform: translateY(0); }
  }

  .intro {
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.6;
    margin: 16px 0;
  }

  .intro strong {
    color: var(--text-primary);
  }

  /* Action Box */
  .action-box {
    text-align: center;
    margin: 24px 0;
    padding: 20px;
    background: var(--bg-elevated);
    border-radius: 12px;
  }

  .action-btn {
    display: inline-flex;
    align-items: center;
    gap: 10px;
    padding: 14px 28px;
    background: linear-gradient(135deg, var(--color-optimal), #10b981);
    color: white;
    border-radius: 12px;
    font-size: 15px;
    font-weight: 600;
    text-decoration: none;
    transition: all 0.2s ease;
    box-shadow: 0 4px 14px rgba(34, 197, 94, 0.3);
  }

  .action-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(34, 197, 94, 0.4);
  }

  .action-hint {
    font-size: 12px;
    color: var(--text-muted);
    margin-top: 10px;
  }

  /* Checklist */
  .checklist {
    margin: 16px 0;
  }

  .check-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 16px;
    background: var(--bg-elevated);
    border-radius: 10px;
    cursor: pointer;
    transition: all 0.15s ease;
    border: 1px solid transparent;
  }

  .check-item:hover {
    background: var(--bg-hover);
    border-color: var(--border-subtle);
  }

  .check-item.checked {
    background: var(--color-optimal-soft);
    border-color: var(--color-optimal);
  }

  .check-item input {
    display: none;
  }

  .checkmark {
    width: 22px;
    height: 22px;
    border: 2px solid var(--border-default);
    border-radius: 6px;
    flex-shrink: 0;
    position: relative;
    transition: all 0.15s;
  }

  .check-item.checked .checkmark {
    background: var(--color-optimal);
    border-color: var(--color-optimal);
  }

  .check-item.checked .checkmark::after {
    content: '';
    position: absolute;
    left: 6px;
    top: 2px;
    width: 6px;
    height: 11px;
    border: solid white;
    border-width: 0 2.5px 2.5px 0;
    transform: rotate(45deg);
  }

  .check-item span:last-child {
    font-size: 14px;
    color: var(--text-secondary);
  }

  .check-item.checked span:last-child {
    color: var(--text-primary);
    font-weight: 500;
  }

  /* Numbered Steps */
  .numbered-steps {
    display: flex;
    flex-direction: column;
    gap: 14px;
    margin: 20px 0;
  }

  .numbered-step {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.5;
  }

  .numbered-step .num {
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-elevated);
    border-radius: 6px;
    font-size: 12px;
    font-weight: 600;
    color: var(--text-muted);
    flex-shrink: 0;
  }

  .numbered-step strong {
    color: var(--text-primary);
  }

  .numbered-step code {
    background: var(--bg-elevated);
    padding: 2px 6px;
    border-radius: 4px;
    font-family: ui-monospace, monospace;
    font-size: 13px;
  }

  .numbered-step a {
    color: var(--color-optimal);
    text-decoration: underline;
  }

  .link-highlight {
    display: inline-block;
    background: var(--color-optimal-soft);
    color: var(--color-optimal) !important;
    text-decoration: none !important;
    padding: 4px 10px;
    border-radius: 6px;
    font-weight: 600;
    font-family: ui-monospace, monospace;
    border: 1px solid var(--color-optimal);
    transition: all 0.15s ease;
  }

  .link-highlight:hover {
    background: var(--color-optimal);
    color: white !important;
  }

  /* Expandable */
  .expandable {
    margin: 16px 0;
    background: var(--bg-elevated);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
    overflow: hidden;
    transition: border-color 0.15s;
  }

  .expandable:hover {
    border-color: var(--color-optimal);
  }

  .expandable summary {
    padding: 14px 16px;
    cursor: pointer;
    font-size: 14px;
    color: var(--text-secondary);
    list-style: none;
    transition: background 0.15s, color 0.15s;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .expandable summary:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
  }

  .expandable summary::-webkit-details-marker {
    display: none;
  }

  .expandable summary::after {
    content: '';
    width: 20px;
    height: 20px;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='20' height='20' viewBox='0 0 24 24' fill='none' stroke='%236b7280' stroke-width='2'%3E%3Cpolyline points='6,9 12,15 18,9'/%3E%3C/svg%3E");
    background-repeat: no-repeat;
    background-position: center;
    transition: transform 0.2s ease;
    flex-shrink: 0;
  }

  .expandable[open] summary::after {
    transform: rotate(180deg);
  }

  .field-list {
    list-style: none;
    padding: 0 16px 16px;
    margin: 0;
  }

  .field-list li {
    font-size: 13px;
    color: var(--text-secondary);
    padding: 8px 0;
    border-bottom: 1px solid var(--border-subtle);
  }

  .field-list li:last-child {
    border-bottom: none;
  }

  .field-list strong {
    color: var(--text-primary);
  }

  /* Note */
  .note {
    font-size: 13px;
    color: var(--text-muted);
    margin: 16px 0;
    padding: 14px;
    background: var(--bg-elevated);
    border-radius: 10px;
    border-left: 3px solid var(--color-optimal);
  }

  /* Buttons */
  .complete-btn {
    width: 100%;
    padding: 14px;
    background: linear-gradient(135deg, var(--color-optimal), #10b981);
    color: white;
    border: none;
    border-radius: 12px;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s ease;
    margin-top: 16px;
    box-shadow: 0 2px 8px rgba(34, 197, 94, 0.2);
  }

  .complete-btn:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(34, 197, 94, 0.3);
  }

  .complete-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
    box-shadow: none;
  }

  .complete-btn.secondary {
    background: var(--bg-elevated);
    color: var(--text-secondary);
    box-shadow: none;
  }

  .complete-btn.secondary:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
  }

  .button-row {
    display: flex;
    gap: 12px;
    margin-top: 16px;
  }

  .button-row .complete-btn {
    flex: 1;
    margin-top: 0;
  }

  /* Completion Card */
  .completion-card {
    text-align: center;
    padding: 36px 28px;
    background: var(--bg-surface);
    border: 2px solid var(--color-optimal);
    border-radius: 20px;
    margin-top: 32px;
    position: relative;
    overflow: hidden;
    animation: popIn 0.4s ease;
  }

  @keyframes popIn {
    0% { transform: scale(0.9); opacity: 0; }
    50% { transform: scale(1.02); }
    100% { transform: scale(1); opacity: 1; }
  }

  .completion-confetti {
    position: absolute;
    inset: 0;
    pointer-events: none;
  }

  .completion-confetti span {
    position: absolute;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    animation: confetti 3s ease-out infinite;
  }

  .completion-confetti span:nth-child(1) { left: 10%; background: var(--color-optimal); animation-delay: 0s; }
  .completion-confetti span:nth-child(2) { left: 30%; background: var(--color-attention); animation-delay: 0.3s; }
  .completion-confetti span:nth-child(3) { left: 50%; background: var(--color-optimal); animation-delay: 0.6s; }
  .completion-confetti span:nth-child(4) { left: 70%; background: #60a5fa; animation-delay: 0.9s; }
  .completion-confetti span:nth-child(5) { left: 90%; background: var(--color-attention); animation-delay: 1.2s; }

  @keyframes confetti {
    0% { transform: translateY(-20px) rotate(0deg); opacity: 1; }
    100% { transform: translateY(100px) rotate(360deg); opacity: 0; }
  }

  .completion-icon {
    width: 64px;
    height: 64px;
    margin: 0 auto 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, var(--color-optimal), #10b981);
    border-radius: 50%;
    color: white;
    box-shadow: 0 4px 20px rgba(34, 197, 94, 0.3);
  }

  .completion-card h2 {
    font-size: 22px;
    font-weight: 700;
    color: var(--text-primary);
    margin-bottom: 8px;
  }

  .completion-card > p {
    font-size: 14px;
    color: var(--text-secondary);
    margin-bottom: 24px;
  }

  /* App Preview */
  .app-preview {
    background: var(--bg-elevated);
    border-radius: 12px;
    padding: 16px;
    margin-bottom: 24px;
    border: 1px solid var(--border-subtle);
  }

  .preview-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
  }

  .preview-dot {
    width: 8px;
    height: 8px;
    background: var(--color-optimal);
    border-radius: 50%;
  }

  .preview-title {
    font-size: 12px;
    font-weight: 600;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .preview-metrics {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 24px;
    margin-bottom: 12px;
  }

  .preview-metric {
    text-align: center;
  }

  .preview-metric .metric-value {
    font-size: 32px;
    font-weight: 700;
    color: var(--color-optimal);
    display: block;
    line-height: 1;
  }

  .preview-metric .metric-label {
    font-size: 11px;
    color: var(--text-muted);
    text-transform: uppercase;
    font-weight: 600;
  }

  .preview-divider {
    width: 1px;
    height: 40px;
    background: var(--border-subtle);
  }

  .preview-bar {
    display: flex;
    height: 8px;
    border-radius: 4px;
    overflow: hidden;
    background: var(--bg-surface);
  }

  .bar-left {
    background: var(--color-optimal);
    border-right: 1px solid var(--bg-surface);
  }

  .bar-right {
    background: var(--color-optimal);
    opacity: 0.7;
  }

  .color-hint {
    display: flex;
    justify-content: center;
    gap: 20px;
    font-size: 13px;
    color: var(--text-muted);
    margin-bottom: 24px;
    flex-wrap: wrap;
  }

  .color-dot {
    display: inline-block;
    width: 10px;
    height: 10px;
    border-radius: 50%;
    margin-right: 6px;
    vertical-align: middle;
  }

  .color-dot.optimal { background: var(--color-optimal); }
  .color-dot.attention { background: var(--color-attention); }
  .color-dot.problem { background: var(--color-problem); }

  .completion-links {
    display: flex;
    justify-content: center;
    gap: 12px;
    flex-wrap: wrap;
  }

  .completion-link {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 12px 20px;
    border-radius: 10px;
    font-size: 14px;
    font-weight: 600;
    text-decoration: none;
    transition: all 0.15s;
  }

  .completion-link.primary {
    background: linear-gradient(135deg, var(--color-optimal), #10b981);
    color: white;
    box-shadow: 0 2px 10px rgba(34, 197, 94, 0.25);
  }

  .completion-link.primary:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 14px rgba(34, 197, 94, 0.35);
  }

  .completion-link.secondary {
    background: var(--bg-elevated);
    color: var(--text-primary);
    border: 1px solid var(--border-subtle);
  }

  .completion-link.secondary:hover {
    border-color: var(--border-default);
    background: var(--bg-hover);
  }

  /* Mobile */
  @media (max-width: 500px) {
    .header {
      padding: 12px 16px;
    }

    .main {
      padding: 24px 16px 48px;
    }

    .hero h1 {
      font-size: 26px;
    }

    .hero-subtitle {
      font-size: 15px;
    }

    .completion-links {
      flex-direction: column;
    }

    .color-hint {
      flex-direction: column;
      gap: 8px;
    }

    .preview-metrics {
      gap: 16px;
    }

    .preview-metric .metric-value {
      font-size: 28px;
    }
  }
</style>
